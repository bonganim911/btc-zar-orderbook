package com.orderbook.api

import com.orderbook.core.OrderBook
import com.orderbook.core.OrderBookLevel
import com.orderbook.exception.ApiError
import com.orderbook.exception.OrderBookException
import com.orderbook.model.*
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import java.util.*

class OrderBookController(
    private val vertx: Vertx,
    private val orderBook: OrderBook
) {
    fun setupRouter(): Router {
        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())

        router.get("/:pair/orderbook").handler(this::handleGetOrderBook)
        router.post("/orders/limit").handler(this::handleSubmitLimitOrder)
        router.get("/:pair/tradehistory").handler(this::handleGetTradeHistory)
        router.get("/orders/:orderId").handler(this::handleGetOrder)

        return router
    }

    private fun handleGetOrderBook(ctx: RoutingContext) {
        try {
            val pair = CurrencyPair.fromString(ctx.pathParam("pair"))
            val depth = ctx.queryParam("depth").firstOrNull()?.toIntOrNull() ?: Int.MAX_VALUE

            val orderBookSnapshot = orderBook.getOrderBook(pair, depth)
                ?: throw OrderBookException.OrderBookNotFound("Order book not found")

            val response = mapOf(
                "success" to true,
                "data" to mapOf(
                    "asks" to orderBookSnapshot.asks.map { listOf(it.price.value, it.quantity.value) },
                    "bids" to orderBookSnapshot.bids.map { listOf(it.price.value, it.quantity.value) },
                    "lastUpdated" to orderBookSnapshot.lastUpdated.toString()
                )
            )

            ctx.response()
                .setStatusCode(200)
                .putHeader("content-type", "application/json")
                .end(Json.encode(response))
        } catch (e: IllegalArgumentException) {
            respondWithError(ctx, ApiError.BadRequest("Invalid currency pair format"))
        } catch (e: OrderBookException.InvalidCurrencyPair) {
            respondWithError(ctx, ApiError.BadRequest(e.message ?: "Invalid currency pair"))
        } catch (e: OrderBookException.OrderBookNotFound) {
            respondWithError(ctx, ApiError.NotFound(e.message ?: "Order book not found"))
        }
    }

    private fun handleSubmitLimitOrder(ctx: RoutingContext) {
        try {
            val body = ctx.bodyAsJson

            if (!validateRequiredFields(body, "pair", "side", "price", "quantity")) {
                respondWithError(ctx, ApiError.BadRequest("Missing required fields"))
                return
            }

            val order = parseOrder(body)
            val result = orderBook.addOrder(order)

            result.fold(
                onSuccess = { trades ->
                    val response = mapOf(
                        "success" to true,
                        "data" to mapOf(
                            "id" to order.id.value,
                            "pair" to order.pair.value.replace("/", ""),
                            "side" to order.side.name,
                            "price" to order.price.value,
                            "quantity" to order.quantity.value
                        )
                    )
                    ctx.response()
                        .setStatusCode(201)
                        .putHeader("content-type", "application/json")
                        .end(Json.encode(response))
                },
                onFailure = { e ->
                    when (e) {
                        is OrderBookException -> respondWithError(ctx, ApiError.BadRequest(e.message ?: "Invalid order"))
                        else -> respondWithError(ctx, ApiError.InternalServerError("Internal server error"))
                    }
                }
            )
        } catch (e: IllegalArgumentException) {
            respondWithError(ctx, ApiError.BadRequest(e.message ?: "Invalid request body"))
        }
    }

    private fun handleGetTradeHistory(ctx: RoutingContext) {
        try {
            val pair = CurrencyPair.fromString(ctx.pathParam("pair"))
            val limit = ctx.queryParam("limit").firstOrNull()?.toIntOrNull() ?: 100

            val trades = orderBook.getTradeHistory(pair, limit)
            val response = mapOf(
                "success" to true,
                "data" to trades.map {
                    mapOf(
                        "price" to it.price.value,
                        "quantity" to it.quantity.value,
                        "currencyPair" to it.pair.value,
                        "tradedAt" to it.createdAt.toString(),
                        "takerSide" to it.takerSide.name
                    )
                }
            )

            ctx.response()
                .setStatusCode(200)
                .putHeader("content-type", "application/json")
                .end(Json.encode(response))
        } catch (e: IllegalArgumentException) {
            respondWithError(ctx, ApiError.BadRequest("Invalid currency pair format"))
        } catch (e: OrderBookException.InvalidCurrencyPair) {
            respondWithError(ctx, ApiError.BadRequest(e.message ?: "Invalid currency pair"))
        }
    }

    private fun handleGetOrder(ctx: RoutingContext) {
        try {
            val orderId = OrderId(ctx.pathParam("orderId"))
            val order = orderBook.getOrder(orderId)

            if (order == null) {
                respondWithError(ctx, ApiError.NotFound("Order not found"))
            } else {
                respondWithSuccess(ctx, 200, order)
            }
        } catch (e: Exception) {
            respondWithError(ctx, ApiError.BadRequest("Invalid order ID"))
        }
    }

    private fun parseOrder(body: JsonObject): Order {
        val pair = CurrencyPair.fromString(body.getString("pair"))
        val side = Order.Side.valueOf(body.getString("side").uppercase())
        val price = body.getDouble("price").takeIf { it > 0 }
            ?: throw IllegalArgumentException("Price must be positive")
        val quantity = body.getDouble("quantity").takeIf { it > 0 }
            ?: throw IllegalArgumentException("Quantity must be positive")

        return Order(
            id = OrderId("order-${UUID.randomUUID()}"),
            pair = pair,
            side = side,
            price = Price(price),
            quantity = Quantity(quantity)
        )
    }

    private fun validateRequiredFields(body: JsonObject, vararg fields: String): Boolean {
        return fields.all { body.getValue(it) != null }
    }

    private fun respondWithSuccess(ctx: RoutingContext, statusCode: Int, data: Any? = null) {
        val response = mutableMapOf<String, Any>(
            "success" to true,
            "status" to statusCode
        )
        data?.let { response["data"] = it }

        ctx.response()
            .setStatusCode(statusCode)
            .putHeader("content-type", "application/json")
            .end(Json.encode(response))
    }

    private fun respondWithError(ctx: RoutingContext, error: ApiError) {
        val response = mapOf(
            "success" to false,
            "error" to error.message,
            "status" to error.statusCode
        )
        ctx.response()
            .setStatusCode(error.statusCode)
            .putHeader("content-type", "application/json")
            .end(Json.encode(response))
    }
}
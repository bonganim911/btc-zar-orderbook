package com.orderbook

import com.orderbook.api.OrderBookController
import com.orderbook.config.AppConfig
import com.orderbook.core.OrderBookProcessor
import com.orderbook.model.CurrencyPair
import io.vertx.core.Vertx

fun main() {
    val vertx = Vertx.vertx()
    val orderBook = OrderBookProcessor(CurrencyPair.fromString(AppConfig.DEFAULT_CURRENCY_PAIR))
    val orderBookController = OrderBookController(vertx, orderBook)

    val server = vertx.createHttpServer()
    server.requestHandler(orderBookController.setupRouter())

    server.listen(AppConfig.DEFAULT_PORT) { result ->
        if (result.succeeded()) {
            println("Server started on port ${AppConfig.DEFAULT_PORT}")
        } else {
            println("Failed to start server: ${result.cause()}")
        }
    }
}
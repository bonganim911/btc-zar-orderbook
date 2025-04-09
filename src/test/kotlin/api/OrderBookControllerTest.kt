package api

import com.orderbook.api.OrderBookController
import com.orderbook.core.OrderBook
import com.orderbook.core.OrderBookLevel
import com.orderbook.core.OrderBookSnapshot
import com.orderbook.model.CurrencyPair
import com.orderbook.model.Order
import com.orderbook.model.Price
import com.orderbook.model.Quantity
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.eq
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(VertxExtension::class)
class OrderBookControllerTest {
    private lateinit var orderBook: OrderBook
    private lateinit var controller: OrderBookController
    private lateinit var vertx: Vertx
    private val btcZar = CurrencyPair.fromString("BTCZAR")

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> any(): T {
        return Mockito.any<T>() ?: when (T::class) {
            CurrencyPair::class -> btcZar as T
            Order::class -> mock(Order::class.java) as T
            else -> null as T
        }
    }


    private fun anyInt(): Int = Mockito.anyInt()

    @BeforeEach
    fun setup(testContext: VertxTestContext) {
        vertx = Vertx.vertx()
        orderBook = mock(OrderBook::class.java)
        controller = OrderBookController(vertx, orderBook)
        testContext.completeNow()
    }

    @AfterEach
    fun cleanup(testContext: VertxTestContext) {
        vertx.close(testContext.succeeding { testContext.completeNow() })
    }

    @Test
    fun `should submit limit order`(testContext: VertxTestContext) {
        `when`(orderBook.getOrderBook(any(), anyInt())).thenReturn(
            OrderBookSnapshot(
                bids = listOf(OrderBookLevel(Price(100.0), Quantity(1.0))),
                asks = listOf(OrderBookLevel(Price(101.0), Quantity(2.0))),
                lastUpdated = Date()
            )
        )
        val orderJson = JsonObject()
            .put("pair", "BTC/ZAR")
            .put("side", "BUY")
            .put("price", 100.0)
            .put("quantity", 1.0)

        vertx.createHttpServer()
            .requestHandler(controller.setupRouter())
            .listen(0)
            .onSuccess { server ->
                val client = WebClient.create(vertx)
                client.post(server.actualPort(), "localhost", "/orders/limit")
                    .sendJsonObject(orderJson, testContext.succeeding { response ->
                        testContext.verify {
                            assertEquals(201, response.statusCode())
                            val body = response.bodyAsJsonObject()
                            val data = body.getJsonObject("data")
                            assertNotNull(data)
                            assertEquals("BTCZAR", data.getString("pair"))
                            assertEquals("BUY", data.getString("side"))
                        }
                        server.close(testContext.succeeding { testContext.completeNow() })
                    })
            }
            .onFailure(testContext::failNow)
    }

    @Test
    fun `should get order book`(testContext: VertxTestContext) {
        `when`(orderBook.getOrderBook(any(), anyInt())).thenReturn(
            OrderBookSnapshot(
                bids = listOf(OrderBookLevel(Price(100.0), Quantity(1.0))),
                asks = listOf(OrderBookLevel(Price(101.0), Quantity(2.0))),
                lastUpdated = Date()
            )
        )

        vertx.createHttpServer()
            .requestHandler(controller.setupRouter())
            .listen(0)
            .onSuccess { server ->
                val client = WebClient.create(vertx)
                client.get(server.actualPort(), "localhost", "/BTCZAR/orderbook")
                    .send(testContext.succeeding { response ->
                        testContext.verify {
                            assertEquals(200, response.statusCode())
                        }
                        server.close(testContext.succeeding { testContext.completeNow() })
                    })
            }
            .onFailure(testContext::failNow)
    }

    @Test
    fun `should get trade history`(testContext: VertxTestContext) {
        `when`(orderBook.getTradeHistory(eq(btcZar), anyInt())).thenReturn(emptyList())

        vertx.createHttpServer()
            .requestHandler(controller.setupRouter())
            .listen(0)
            .onSuccess { server ->
                val client = WebClient.create(vertx)
                client.get(server.actualPort(), "localhost", "/BTCZAR/tradehistory")
                    .send(testContext.succeeding { response ->
                        testContext.verify {
                            assertEquals(200, response.statusCode())
                            val body = response.bodyAsJsonObject()
                            assertEquals(true, body.getBoolean("success"))
                        }
                        server.close(testContext.succeeding { testContext.completeNow() })
                    })
            }
            .onFailure(testContext::failNow)
    }

    @Test
    fun `should reject invalid currency pair format`(testContext: VertxTestContext) {
        vertx.createHttpServer()
            .requestHandler(controller.setupRouter())
            .listen(0)
            .onSuccess { server ->
                val client = WebClient.create(vertx)
                client.get(server.actualPort(), "localhost", "/BTCUSD/orderbook")
                    .send(testContext.succeeding { response ->
                        testContext.verify {
                            assertEquals(404, response.statusCode())
                            assertNotNull(response.bodyAsJsonObject().getString("error"))
                        }
                        server.close(testContext.succeeding { testContext.completeNow() })
                    })
            }
            .onFailure(testContext::failNow)
    }

    @Test
    fun `should return 404 when order book not found`(testContext: VertxTestContext) {
        `when`(orderBook.getOrderBook(eq(btcZar), anyInt())).thenReturn(null)

        vertx.createHttpServer()
            .requestHandler(controller.setupRouter())
            .listen(0)
            .onSuccess { server ->
                val client = WebClient.create(vertx)
                client.get(server.actualPort(), "localhost", "/BTCZAR/orderbook")
                    .send(testContext.succeeding { response ->
                        testContext.verify {
                            assertEquals(404, response.statusCode())
                        }
                        server.close(testContext.succeeding { testContext.completeNow() })
                    })
            }
            .onFailure(testContext::failNow)
    }
}


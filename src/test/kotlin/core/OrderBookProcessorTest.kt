package core

import com.orderbook.core.OrderBookProcessor
import com.orderbook.model.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.*

class OrderBookProcessorTest {
    private lateinit var orderBook: OrderBookProcessor
    private val btcZar = CurrencyPair.fromString("BTC/ZAR")

    @BeforeEach
    fun setup() {
        orderBook = OrderBookProcessor(btcZar)
    }

    @Test
    fun `should add buy order to empty book`() {
        val order = createOrder(Order.Side.BUY, 100.0, 1.0)
        val trades = orderBook.addOrder(order).getOrThrow()

        assertTrue(trades.isEmpty())
        assertEquals(1, orderBook.getOrderBook(btcZar, 1).bids.size)
        assertEquals(0, orderBook.getOrderBook(btcZar, 1).asks.size)
    }

    @Test
    fun `should add sell order to empty book`() {
        val order = createOrder(Order.Side.SELL, 100.0, 1.0)
        val trades = orderBook.addOrder(order).getOrThrow()

        assertTrue(trades.isEmpty())
        assertEquals(0, orderBook.getOrderBook(btcZar, 1).bids.size)
        assertEquals(1, orderBook.getOrderBook(btcZar, 1).asks.size)
    }

    @Test
    fun `should match buy order with sell order`() {
        val sellOrder = createOrder(Order.Side.SELL, 100.0, 1.0)
        orderBook.addOrder(sellOrder)


        val buyOrder = createOrder(Order.Side.BUY, 100.0, 1.0)
        val trades = orderBook.addOrder(buyOrder).getOrThrow()

        assertEquals(1, trades.size)
        assertEquals(100.0, trades[0].price.value)
        assertEquals(1.0, trades[0].quantity.value)
        assertEquals(Order.Side.BUY, trades[0].takerSide)
    }

    @Test
    fun `should partially match orders`() {
        orderBook.addOrder(createOrder(Order.Side.SELL, 100.0, 2.0))

        val trades = orderBook.addOrder(createOrder(Order.Side.BUY, 100.0, 1.0)).getOrThrow()

        assertEquals(1, trades.size)
        assertEquals(1.0, trades[0].quantity.value)

        val book = orderBook.getOrderBook(btcZar, 1)
        assertEquals(1, book.bids.size)
        assertEquals(1, book.asks.size)
        assertEquals(2.0, book.asks[0].quantity.value)
    }

    @Test
    fun `should prioritize better prices`() {
        val quantity = 1.0
        orderBook.addOrder(createOrder(Order.Side.SELL, 101.0, quantity))
        orderBook.addOrder(createOrder(Order.Side.SELL, 100.0, quantity))
        orderBook.addOrder(createOrder(Order.Side.SELL, 99.0, quantity))


        val trades = orderBook.addOrder(createOrder(Order.Side.BUY, 100.0, 2.0)).getOrThrow()

        assertEquals(2, trades.size)
        assertEquals(99.0, trades[0].price.value)
        assertEquals(100.0, trades[1].price.value)

        val book = orderBook.getOrderBook(btcZar, 2)
        assertEquals(2, book.asks.size)
        assertEquals(101.0, book.asks[1].price.value)
    }

    @Test
    fun `should return correct order book snapshot`() {
        orderBook.addOrder(createOrder(Order.Side.BUY, 99.0, 2.0))
        orderBook.addOrder(createOrder(Order.Side.BUY, 100.0, 1.0))
        orderBook.addOrder(createOrder(Order.Side.SELL, 101.0, 3.0))
        orderBook.addOrder(createOrder(Order.Side.SELL, 102.0, 2.0))

        val snapshot = orderBook.getOrderBook(btcZar, 2)


        assertEquals(2, snapshot.bids.size)
        assertEquals(100.0, snapshot.bids[0].price.value)
        assertEquals(99.0, snapshot.bids[1].price.value)


        assertEquals(2, snapshot.asks.size)
        assertEquals(101.0, snapshot.asks[0].price.value)
        assertEquals(102.0, snapshot.asks[1].price.value)
    }

    @Test
    fun `should return trade history`() {
        val priceOfHundred = 100.0
        orderBook.addOrder(createOrder(Order.Side.SELL, priceOfHundred, 1.0))
        orderBook.addOrder(createOrder(Order.Side.BUY, priceOfHundred, 1.0))

        val priceOfNinetyNine = 99.0
        orderBook.addOrder(createOrder(Order.Side.BUY, priceOfNinetyNine, 2.0))
        orderBook.addOrder(createOrder(Order.Side.SELL, priceOfNinetyNine, 1.0))

        val history = orderBook.getTradeHistory(btcZar,3)

        assertEquals(3, history.size)
        println(history[0].price.value)
        assertEquals(priceOfNinetyNine, history[0].price.value)
        assertEquals(priceOfHundred, history[1].price.value)
    }

    @Test
    fun `should return cached order book snapshot`() {
        orderBook.addOrder(createOrder(Order.Side.BUY, 100.0, 1.0))
        val firstCall = orderBook.getOrderBook(btcZar, 1)
        val secondCall = orderBook.getOrderBook(btcZar, 1)

        assertSame(firstCall, secondCall, "Should return cached instance")
    }

    @Test
    fun `should invalidate cache when new order added`() {
        orderBook.addOrder(createOrder(Order.Side.BUY, 100.0, 1.0))
        val firstCall = orderBook.getOrderBook(btcZar, 1)
        orderBook.addOrder(createOrder(Order.Side.SELL, 101.0, 1.0))
        val secondCall = orderBook.getOrderBook(btcZar, 1)

        assertNotSame(firstCall, secondCall, "Should invalidate cache")
    }

    private fun createOrder(side: Order.Side, price: Double, quantity: Double): Order {
        return Order(
            id = OrderId("order-${UUID.randomUUID()}"),
            pair = btcZar,
            side = side,
            price = Price(price),
            quantity = Quantity(quantity)
        )
    }
}
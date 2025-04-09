package model

import com.orderbook.model.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class OrderTest {
    @Test
    fun `should create valid order`() {
        val order = Order(
            id = OrderId("order-1"),
            pair = CurrencyPair.fromString("BTC/ZAR"),
            side = Order.Side.BUY,
            price = Price(100.0),
            quantity = Quantity(1.0)
        )

        assertEquals("order-1", order.id.value)
        assertEquals("BTC/ZAR", order.pair.value)
        assertEquals(Order.Side.BUY, order.side)
        assertEquals(100.0, order.price.value)
        assertEquals(1.0, order.quantity.value)
    }

    @Test
    fun `should create order with alternative currency pair format`() {
        val order = Order(
            id = OrderId("order-1"),
            pair = CurrencyPair.fromString("BTCZAR"),
            side = Order.Side.SELL,
            price = Price(100.0),
            quantity = Quantity(1.0)
        )

        assertEquals("BTC/ZAR", order.pair.value)
    }

    @Test
    fun `should update quantity immutably`() {
        val original = Order(
            id = OrderId("order-1"),
            pair = CurrencyPair.fromString("BTC/ZAR"),
            side = Order.Side.BUY,
            price = Price(100.0),
            quantity = Quantity(1.0)
        )

        val updated = original.withNewQuantity(Quantity(0.5))

        assertEquals(1.0, original.quantity.value)
        assertEquals(0.5, updated.quantity.value)
    }
}
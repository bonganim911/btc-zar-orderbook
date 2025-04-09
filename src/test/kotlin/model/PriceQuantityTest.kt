package model

import com.orderbook.model.Price
import com.orderbook.model.Quantity
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PriceQuantityTest {
    @Test
    fun `should create valid price and quantity`() {
        val price = Price(100.0)
        val quantity = Quantity(1.5)

        assertEquals(100.0, price.value)
        assertEquals(1.5, quantity.value)
    }

    @Test
    fun `should reject non-positive values`() {
        assertThrows<IllegalArgumentException> { Price(-1.0) }
        assertThrows<IllegalArgumentException> { Price(0.0) }
        assertThrows<IllegalArgumentException> { Quantity(-0.5) }
    }

    @Test
    fun `should compare prices correctly`() {
        assertTrue(Price(100.0) < Price(200.0))
        assertEquals(Price(50.0), Price(50.0))
    }
}
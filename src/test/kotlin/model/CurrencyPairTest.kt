package model

import com.orderbook.model.CurrencyPair
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class CurrencyPairTest {
    @Test
    fun `should accept valid currency pairs`() {
        val pairs = listOf("BTC/ZAR", "ETH/USD", "XRPZAR", "USDEUR")

        pairs.forEach { pairStr ->
            val pair = CurrencyPair.fromString(pairStr)
            assertEquals(pairStr.replace(Regex("([A-Z]{3})([A-Z]{3})"), "$1/$2"), pair.value)
        }
    }

    @Test
    fun `should reject invalid currency pairs`() {
        val invalidPairs = listOf("BTCZAR1", "BTC-ZAR", "btc/zar", "BTC", "BTC/USD/EXTRA")

        invalidPairs.forEach { pairStr ->
            assertThrows<IllegalArgumentException> {
                CurrencyPair.fromString(pairStr)
            }
        }
    }
}
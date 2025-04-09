package com.orderbook.model

import java.util.*


data class Order (
    val id: OrderId,
    val pair: CurrencyPair,
    val side: Side,
    val price: Price,
    val quantity: Quantity,
    val createdAt: Date = Date()
    ) {

    enum class Side { BUY, SELL}
    fun withNewQuantity(newQuantity: Quantity) = copy(quantity = newQuantity)
}

@JvmInline
value class OrderId(val value: String)

@JvmInline
value class CurrencyPair(val value: String) {
    init {
        require(value.matches(Regex("[A-Z]{3,4}/[A-Z]{3,4}")) ||
                value.matches(Regex("[A-Z]{3,4}[A-Z]{3,4}"))) {
            "Invalid currency pair format. Expected formats: ABC/XYZ or ABCXYZ"
        }
    }

    companion object {
        fun fromString(pair: String): CurrencyPair {
            // Convert BTCZAR to BTC/ZAR if needed
            val formattedPair = if (!pair.contains('/') && pair.length == 6) {
                "${pair.substring(0, 3)}/${pair.substring(3)}"
            } else {
                pair
            }
            return CurrencyPair(formattedPair)
        }
    }
}
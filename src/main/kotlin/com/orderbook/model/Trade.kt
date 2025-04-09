package com.orderbook.model

import java.util.*

data class Trade(
    val id: TradeId,
    val pair: CurrencyPair,
    val price: Price,
    val quantity: Quantity,
    val takerSide: Order.Side,
    val createdAt: Date = Date()
)

@JvmInline
value class TradeId(val value: String)
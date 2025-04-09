package com.orderbook.core

import com.orderbook.model.*
import java.util.*

interface OrderBook {
    fun addOrder(order: Order): Result<List<Trade>>
    fun getOrderBook(pair: CurrencyPair, depth: Int): OrderBookSnapshot
    fun getTradeHistory(pair: CurrencyPair, limit: Int): List<Trade>
    fun getOrder(orderId: OrderId): Order?
}

data class OrderBookSnapshot(
    val bids: List<OrderBookLevel>,
    val asks: List<OrderBookLevel>,
    val lastUpdated: Date = Date()
)

data class OrderBookLevel(
    val price: Price,
    val quantity: Quantity
)
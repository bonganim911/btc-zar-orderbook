package com.orderbook.exception

import com.orderbook.model.OrderId

sealed class OrderBookException(message: String) : RuntimeException(message) {
    data class InvalidOrder(val reason: String) : OrderBookException("Invalid order: $reason")
    data class OrderNotFound(val orderId: OrderId) : OrderBookException("Order not found: ${orderId.value}")
    data class OrderBookNotFound(override val message: String) : OrderBookException("Order book not found")
    data class InvalidCurrencyPair(val pair: String) : OrderBookException("Invalid currency pair: $pair")
}

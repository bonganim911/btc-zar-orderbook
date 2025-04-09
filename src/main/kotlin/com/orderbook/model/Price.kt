package com.orderbook.model

@JvmInline
value class Price (val value: Double): Comparable<Price> {
    init {
        require(value > 0) { "Price must be greater than zero"}
    }
    override fun compareTo(other: Price): Int = value.compareTo(other.value)
}
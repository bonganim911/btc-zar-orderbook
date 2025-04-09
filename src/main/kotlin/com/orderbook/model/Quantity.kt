package com.orderbook.model

@JvmInline
value class Quantity(val value: Double) : Comparable<Quantity> {
    init {
        require(value >= 0) { "Quantity must be non-negative" }
    }

    companion object {
        val ZERO = Quantity(0.0)
    }

    override fun compareTo(other: Quantity): Int = value.compareTo(other.value)
}
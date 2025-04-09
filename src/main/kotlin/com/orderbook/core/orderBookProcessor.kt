package com.orderbook.core

import com.orderbook.exception.OrderBookException
import com.orderbook.model.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock

class OrderBookProcessor(private val pair: CurrencyPair) : OrderBook {
    // Order storage
    private val bids = TreeMap<Price, MutableList<Order>>(compareByDescending { it })
    private val asks = TreeMap<Price, MutableList<Order>>(compareBy { it })
    private val orders = ConcurrentHashMap<OrderId, Order>()

    // Trade history with optimized access
    private val trades = LinkedList<Trade>()
    private var sortedTrades: List<Trade>? = null

    // ID generation
    private val idGenerator = AtomicLong(0)

    // Concurrency control
    private val lock = ReentrantReadWriteLock()

    // Order book snapshot caching
    private var lastOrderBookSnapshot: OrderBookSnapshot? = null
    private var lastOrderBookDepth: Int = -1

    override fun addOrder(order: Order): Result<List<Trade>> {
        lock.writeLock().lock()
        try {
            return runCatching {
                validateOrder(order)
                val executedTrades = mutableListOf<Trade>()

                when (order.side) {
                    Order.Side.BUY -> processOrder(
                        order = order,
                        oppositeBook = asks,
                        priceCondition = { oppositePrice, orderPrice -> oppositePrice > orderPrice },
                        remainingBook = bids,
                        executedTrades = executedTrades
                    )
                    Order.Side.SELL -> processOrder(
                        order = order,
                        oppositeBook = bids,
                        priceCondition = { oppositePrice, orderPrice -> oppositePrice < orderPrice },
                        remainingBook = asks,
                        executedTrades = executedTrades
                    )
                }

                // Invalidate caches
                lastOrderBookSnapshot = null
                sortedTrades = null

                executedTrades
            }
        } finally {
            lock.writeLock().unlock()
        }
    }

    private fun processOrder(
        order: Order,
        oppositeBook: TreeMap<Price, MutableList<Order>>,
        priceCondition: (Price, Price) -> Boolean,
        remainingBook: TreeMap<Price, MutableList<Order>>,
        executedTrades: MutableList<Trade>
    ) {
        var currentQuantity = order.quantity
        val iter = oppositeBook.iterator()

        while (iter.hasNext() && currentQuantity.value > 0) {
            val (price, ordersAtPrice) = iter.next()
            if (priceCondition(price, order.price)) break

            val orderIter = ordersAtPrice.iterator()
            while (orderIter.hasNext() && currentQuantity.value > 0) {
                val matchingOrder = orderIter.next()
                val tradeQuantityValue = minOf(currentQuantity.value, matchingOrder.quantity.value)
                val tradeQuantity = Quantity(tradeQuantityValue)

                executeTrade(order, price, tradeQuantity, executedTrades)
                currentQuantity = calculateRemainingQuantity(currentQuantity, tradeQuantity)
                    ?: break // No remaining quantity

                updateOrderAfterTrade(matchingOrder, tradeQuantity, orderIter)
            }

            if (ordersAtPrice.isEmpty()) iter.remove()
        }

        if (currentQuantity.value > 0) {
            addRemainingOrder(order, currentQuantity, remainingBook)
        }
    }

    private fun calculateRemainingQuantity(current: Quantity, traded: Quantity): Quantity? {
        val remaining = current.value - traded.value
        return if (remaining > 0) Quantity(remaining) else null
    }

    private fun executeTrade(
        takerOrder: Order,
        price: Price,
        quantity: Quantity,
        executedTrades: MutableList<Trade>
    ) {
        val trade = Trade(
            id = TradeId("trade-${idGenerator.incrementAndGet()}"),
            pair = pair,
            price = price,
            quantity = quantity,
            takerSide = takerOrder.side
        )
        trades.addFirst(trade) // Add newest trades at head
        executedTrades.add(trade)
    }

    private fun updateOrderAfterTrade(
        order: Order,
        tradeQuantity: Quantity,
        orderIter: MutableIterator<Order>
    ) {
        val newQuantity = Quantity(order.quantity.value - tradeQuantity.value)
        if (newQuantity.value > 0) {
            orders[order.id] = order.withNewQuantity(newQuantity)
        } else {
            orderIter.remove()
            orders.remove(order.id)
        }
    }

    private fun addRemainingOrder(
        originalOrder: Order,
        remainingQuantity: Quantity,
        book: TreeMap<Price, MutableList<Order>>
    ) {
        val newOrder = originalOrder.copy(quantity = remainingQuantity)
        book.computeIfAbsent(originalOrder.price) { mutableListOf() }.add(newOrder)
        orders[newOrder.id] = newOrder
    }

    override fun getOrderBook(pair: CurrencyPair, depth: Int): OrderBookSnapshot {
        lock.readLock().lock()
        try {
            if (lastOrderBookSnapshot == null || lastOrderBookDepth != depth) {
                lastOrderBookSnapshot = buildOrderBookSnapshot(depth)
                lastOrderBookDepth = depth
            }
            return lastOrderBookSnapshot!!
        } finally {
            lock.readLock().unlock()
        }
    }

    private fun buildOrderBookSnapshot(depth: Int): OrderBookSnapshot {
        val bidsList = bids.entries.flatMap { (price, orders) ->
            orders.map { OrderBookLevel(price, it.quantity) }
        }.take(depth)

        val asksList = asks.entries.flatMap { (price, orders) ->
            orders.map { OrderBookLevel(price, it.quantity) }
        }.take(depth)

        return OrderBookSnapshot(bidsList, asksList)
    }

    override fun getTradeHistory(pair: CurrencyPair, limit: Int): List<Trade> {
        lock.readLock().lock()
        try {
            return sortedTrades?.take(limit) ?: run {
                val sorted = trades.sortedByDescending { it.createdAt }
                sortedTrades = sorted
                sorted.take(limit)
            }
        } finally {
            lock.readLock().unlock()
        }
    }

    override fun getOrder(orderId: OrderId): Order? {
        lock.readLock().lock()
        try {
            return orders[orderId]
        } finally {
            lock.readLock().unlock()
        }
    }

    private fun validateOrder(order: Order) {
        if (order.pair != pair) {
            throw OrderBookException.InvalidCurrencyPair(order.pair.value)
        }
    }
}
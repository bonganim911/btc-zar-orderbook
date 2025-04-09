package com.orderbook.exception

sealed class ApiError(val statusCode: Int, open val message: String) {
    data class BadRequest(override val message: String) : ApiError(400, message)
    data class NotFound(override val message: String) : ApiError(404, message)
    data class InternalServerError(override val message: String) : ApiError(500, message)
}
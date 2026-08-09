package com.englishcoach60.domain.error

enum class AiFailureReason {
    MISSING_API_KEY,
    AUTHENTICATION,
    PAYMENT_REQUIRED,
    RATE_LIMITED,
    TIMEOUT,
    NETWORK,
    INVALID_REQUEST,
    SERVICE_UNAVAILABLE,
    INVALID_RESPONSE,
    UNKNOWN,
}

class AiServiceException(
    val reason: AiFailureReason,
    cause: Throwable? = null,
) : Exception(cause?.message, cause)

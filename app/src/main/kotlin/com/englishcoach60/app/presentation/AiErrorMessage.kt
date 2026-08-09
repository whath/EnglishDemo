package com.englishcoach60.app.presentation

import com.englishcoach60.domain.error.AiFailureReason
import com.englishcoach60.domain.error.AiServiceException

fun Throwable.toUserFacingAiMessage(): String = when ((this as? AiServiceException)?.reason) {
    AiFailureReason.MISSING_API_KEY ->
        "No API key is configured. Add DEEPSEEK_API_KEY to local.properties and rebuild the app."
    AiFailureReason.AUTHENTICATION ->
        "The API key was rejected. Replace it in local.properties and rebuild the app."
    AiFailureReason.PAYMENT_REQUIRED ->
        "The DeepSeek account has insufficient balance. Top it up and try again."
    AiFailureReason.RATE_LIMITED ->
        "Too many AI requests were sent. Wait a moment and try again."
    AiFailureReason.TIMEOUT ->
        "The AI request timed out. Check your connection and try again."
    AiFailureReason.NETWORK ->
        "The AI service couldn't be reached. Check this device's internet connection."
    AiFailureReason.INVALID_REQUEST ->
        "DeepSeek rejected the request. Check the Base URL and model in Settings."
    AiFailureReason.SERVICE_UNAVAILABLE ->
        "DeepSeek is temporarily unavailable. Try again shortly."
    AiFailureReason.INVALID_RESPONSE ->
        "DeepSeek returned an unreadable response. Try the request again."
    AiFailureReason.UNKNOWN, null ->
        "The AI request failed unexpectedly. Try again."
}

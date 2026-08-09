package com.englishcoach60.data.ai

import com.englishcoach60.domain.error.AiFailureReason
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class AiErrorMappingTest {
    @Test
    fun mapsHttpFailuresToSpecificReasons() {
        assertEquals(AiFailureReason.AUTHENTICATION, httpError(401).toAiServiceException().reason)
        assertEquals(AiFailureReason.PAYMENT_REQUIRED, httpError(402).toAiServiceException().reason)
        assertEquals(AiFailureReason.RATE_LIMITED, httpError(429).toAiServiceException().reason)
        assertEquals(AiFailureReason.SERVICE_UNAVAILABLE, httpError(503).toAiServiceException().reason)
        assertEquals(AiFailureReason.INVALID_REQUEST, httpError(400).toAiServiceException().reason)
    }

    @Test
    fun mapsTransportAndPayloadFailuresToSpecificReasons() {
        assertEquals(AiFailureReason.TIMEOUT, SocketTimeoutException().toAiServiceException().reason)
        assertEquals(AiFailureReason.NETWORK, IOException().toAiServiceException().reason)
        assertEquals(
            AiFailureReason.INVALID_RESPONSE,
            SerializationException("invalid payload").toAiServiceException().reason,
        )
    }

    private fun httpError(code: Int): HttpException = HttpException(
        Response.error<Unit>(
            code,
            "{}".toResponseBody("application/json".toMediaType()),
        ),
    )
}

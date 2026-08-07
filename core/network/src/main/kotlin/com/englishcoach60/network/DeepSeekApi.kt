package com.englishcoach60.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

@Serializable data class ChatMessage(val role: String, val content: String)
@Serializable data class ResponseFormat(val type: String = "json_object")
@Serializable data class ThinkingConfig(val type: String = "disabled")
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double,
    @SerialName("max_tokens") val maxTokens: Int,
    @SerialName("response_format") val responseFormat: ResponseFormat = ResponseFormat(),
    val thinking: ThinkingConfig = ThinkingConfig(),
    val stream: Boolean = false,
)
@Serializable data class ChatChoice(val message: ChatMessage)
@Serializable data class ChatResponse(val choices: List<ChatChoice>)

interface DeepSeekApi {
    @POST("chat/completions")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}

object DeepSeekApiFactory {
    val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }
    fun create(baseUrl: String, apiKey: String, debug: Boolean): DeepSeekApi {
        val logging = HttpLoggingInterceptor().apply { level = if (debug) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE }
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging.apply { redactHeader("Authorization") })
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(DeepSeekApi::class.java)
    }
}

object AiJsonSanitizer {
    fun sanitize(raw: String): String {
        val trimmed = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start >= 0 && end >= start) trimmed.substring(start, end + 1) else trimmed
    }
}

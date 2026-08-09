package com.englishcoach60.data.ai

import com.englishcoach60.data.BuildConfig
import com.englishcoach60.domain.error.AiFailureReason
import com.englishcoach60.domain.error.AiServiceException
import com.englishcoach60.domain.model.ConversationContext
import com.englishcoach60.domain.model.ConversationReply
import com.englishcoach60.domain.model.Correction
import com.englishcoach60.domain.model.CorrectionType
import com.englishcoach60.domain.model.DailyLesson
import com.englishcoach60.domain.model.DailyLessonRequest
import com.englishcoach60.domain.model.DailyReview
import com.englishcoach60.domain.model.DailyReviewRequest
import com.englishcoach60.domain.model.Expression
import com.englishcoach60.domain.model.ListeningQuestion
import com.englishcoach60.domain.model.RetellingFeedback
import com.englishcoach60.domain.model.RetellingRequest
import com.englishcoach60.domain.model.SpeakingScenario
import com.englishcoach60.domain.model.WordLookup
import com.englishcoach60.domain.repository.AiRepository
import com.englishcoach60.domain.repository.SettingsRepository
import com.englishcoach60.network.AiJsonSanitizer
import com.englishcoach60.network.ChatMessage
import com.englishcoach60.network.ChatRequest
import com.englishcoach60.network.DeepSeekApiFactory
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

class AiRepositoryImpl(
    private val settingsRepository: SettingsRepository,
    private val json: Json,
    private val scripted: ScriptedConversationProvider,
) : AiRepository {
    private val demo get() = BuildConfig.DEEPSEEK_API_KEY.isBlank()

    override suspend fun generateDailyLesson(request: DailyLessonRequest): DailyLesson {
        if (demo) return DemoContent.lesson(request.day, request.topic, request.difficulty)
        val payload = structuredRequest<LessonPayload>(DailyLessonPromptFactory.create(request), .6, 1800)
        return payload.toDomain(request.day, request.difficulty)
    }

    override suspend fun continueConversation(context: ConversationContext): ConversationReply {
        if (demo) return scripted.reply(context.userMessage, context.turns.count { it.role == "user" })
        return structuredRequest(ConversationPromptFactory.create(context), .7, 500)
    }

    override suspend fun analyzeRetelling(request: RetellingRequest): RetellingFeedback {
        if (demo) return RetellingFeedback(
            summaryZh = "你完成了清晰的自我介绍。继续尝试使用完整句子。",
            correctedVersion = request.transcript,
        )
        return structuredRequest(RetellingPromptFactory.create(request), .3, 900)
    }

    override suspend fun createDailyReview(request: DailyReviewRequest): DailyReview {
        if (demo) return DailyReview(
            day = request.day,
            progressZh = "今天完成了完整的听说训练，并主动用英语表达。",
            mainProblemZh = "继续减少逐句翻译，优先说出短而完整的句子。",
            topMistakes = request.turns.mapNotNull { it.correction }
                .filter { it.type == CorrectionType.IMPORTANT }
                .take(3),
            keyExpressions = request.expressions.take(5),
            tomorrowFocusZh = "明天先快速回忆今天的 5 个核心表达。",
        )
        val payload: ReviewPayload = structuredRequest(DailyReviewPromptFactory.create(request), .3, 1000)
        return payload.toDomain(request.day)
    }

    override suspend fun lookupWord(query: String): WordLookup {
        if (demo) return DemoContent.lookup(query)
        return structuredRequest(WordLookupPromptFactory.create(query), .2, 500)
    }

    override suspend fun testConnection(): Result<Unit> = runCatching {
        if (demo) throw AiServiceException(AiFailureReason.MISSING_API_KEY)
        check(request("Return JSON only: {\"ok\":true}", .0, 30).isNotBlank()) { "Empty AI response" }
    }

    private suspend fun request(prompt: String, temperature: Double, maxTokens: Int): String {
        val settings = settingsRepository.observe().first()
        val api = DeepSeekApiFactory.create(settings.baseUrl, BuildConfig.DEEPSEEK_API_KEY, BuildConfig.DEBUG)
        var last: Throwable? = null
        repeat(2) { attempt ->
            try {
                return api.chat(ChatRequest(settings.model, listOf(ChatMessage("user", prompt)), temperature, maxTokens))
                    .choices.firstOrNull()?.message?.content?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("Empty AI response")
            } catch (error: Throwable) {
                last = error
                val retryable = error is IOException || (error is HttpException && error.code() in 500..599)
                if (!retryable || attempt == 1 || (error is HttpException && error.code() == 401)) {
                    throw error.toAiServiceException()
                }
            }
        }
        throw (last ?: IllegalStateException("AI unavailable")).toAiServiceException()
    }

    private inline fun <reified T> parse(raw: String): T = json.decodeFromString(AiJsonSanitizer.sanitize(raw))

    private suspend inline fun <reified T> structuredRequest(prompt: String, temperature: Double, maxTokens: Int): T {
        val first = request(prompt, temperature, maxTokens)
        return try {
            parse(first)
        } catch (error: SerializationException) {
            val repairPrompt = """
                The previous response was invalid for the required JSON contract.
                Return a corrected JSON object only. Do not use markdown fences or commentary.
                Original task:
                $prompt
                Invalid response:
                ${first.take(8_000)}
            """.trimIndent()
            try {
                parse(request(repairPrompt, temperature, maxTokens))
            } catch (repairError: SerializationException) {
                throw AiServiceException(AiFailureReason.INVALID_RESPONSE, repairError)
            }
        }
    }
}

internal fun Throwable.toAiServiceException(): AiServiceException {
    if (this is AiServiceException) return this
    val reason = when (this) {
        is SocketTimeoutException -> AiFailureReason.TIMEOUT
        is IOException -> AiFailureReason.NETWORK
        is SerializationException -> AiFailureReason.INVALID_RESPONSE
        is HttpException -> when (code()) {
            400, 404, 405, 409, 422 -> AiFailureReason.INVALID_REQUEST
            401, 403 -> AiFailureReason.AUTHENTICATION
            402 -> AiFailureReason.PAYMENT_REQUIRED
            408, 504 -> AiFailureReason.TIMEOUT
            429 -> AiFailureReason.RATE_LIMITED
            in 500..599 -> AiFailureReason.SERVICE_UNAVAILABLE
            else -> AiFailureReason.UNKNOWN
        }
        is IllegalStateException -> AiFailureReason.INVALID_RESPONSE
        else -> AiFailureReason.UNKNOWN
    }
    return AiServiceException(reason, this)
}

@Serializable
private data class LessonPayload(
    val title: String,
    val objectiveZh: String,
    val listeningText: String,
    val translationZh: String,
    val expressions: List<Expression>,
    val questions: List<ListeningQuestion>,
    val speakingScenario: SpeakingScenario,
    val retellingPrompt: String,
) {
    fun toDomain(day: Int, difficulty: Int) = DailyLesson(
        day,
        title,
        objectiveZh,
        listeningText,
        translationZh,
        expressions.take(5).map { it.copy(sourceDay = day) },
        questions.take(3),
        speakingScenario,
        retellingPrompt,
        difficulty,
    )
}

@Serializable
private data class ReviewPayload(
    val progressZh: String,
    val mainProblemZh: String,
    val topMistakes: List<Correction>,
    val keyExpressions: List<Expression>,
    val tomorrowFocusZh: String,
) {
    fun toDomain(day: Int) = DailyReview(
        day,
        progressZh,
        mainProblemZh,
        topMistakes.take(3),
        keyExpressions.take(5),
        tomorrowFocusZh,
    )
}

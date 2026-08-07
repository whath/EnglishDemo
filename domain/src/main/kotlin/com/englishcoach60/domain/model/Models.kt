package com.englishcoach60.domain.model

import kotlinx.serialization.Serializable

enum class DifficultyLevel(val value: Int) { LEVEL_1(1), LEVEL_2(2), LEVEL_3(3), LEVEL_4(4) }
enum class TrainingMode { QUICK, STANDARD, INTENSIVE }
enum class TrainingStatus { NOT_STARTED, IN_PROGRESS, COMPLETED }
enum class TrainingStep { RECALL, LISTENING, REPEAT, SPEAKING, RETELLING, REVIEW }
enum class CorrectionType { NONE, MINOR, IMPORTANT }
enum class ReviewRating { AGAIN, HARD, GOOD }
enum class SourceType { LISTENING, SPEAKING, CORRECTION, RETELLING, MANUAL }

@Serializable
data class Expression(
    val expression: String,
    val meaningZh: String,
    val example: String,
    val sourceDay: Int = 1,
    val sourceType: SourceType = SourceType.LISTENING,
    val mastery: Int = 0,
    val pinned: Boolean = false,
    val nextReviewAt: Long = 0,
    val intervalDays: Int = 0,
)

@Serializable
data class ListeningQuestion(
    val question: String,
    val options: List<String>,
    val answerIndex: Int,
)

@Serializable
data class SpeakingScenario(
    val aiRole: String,
    val userRole: String,
    val goal: String,
    val openingLine: String,
)

@Serializable
data class DailyLesson(
    val day: Int,
    val title: String,
    val objectiveZh: String,
    val listeningText: String,
    val translationZh: String,
    val expressions: List<Expression>,
    val questions: List<ListeningQuestion>,
    val speakingScenario: SpeakingScenario,
    val retellingPrompt: String,
)

@Serializable
data class Correction(
    val type: CorrectionType = CorrectionType.NONE,
    val original: String = "",
    val corrected: String = "",
    val explanationZh: String = "",
)

@Serializable
data class ConversationTurn(
    val id: Long = 0,
    val day: Int,
    val turnIndex: Int,
    val role: String,
    val text: String,
    val correction: Correction? = null,
    val betterExpression: String = "",
    val responseDelayMs: Long = 0,
    val speechDurationMs: Long = 0,
)

@Serializable
data class ConversationReply(
    val replyEnglish: String,
    val correction: Correction = Correction(),
    val betterExpression: String = "",
    val usedTargetExpressions: List<String> = emptyList(),
    val continueConversation: Boolean = true,
)

@Serializable
data class RetellingIssue(val original: String, val better: String, val explanationZh: String)

@Serializable
data class RetellingFeedback(
    val summaryZh: String,
    val correctedVersion: String,
    val topIssues: List<RetellingIssue> = emptyList(),
    val usefulExpressions: List<Expression> = emptyList(),
)

@Serializable
data class WordLookup(
    val word: String,
    val phonetic: String = "",
    val partOfSpeech: String = "",
    val meaningZh: String,
    val definitionEnglish: String = "",
    val example: String,
    val exampleZh: String = "",
    val relatedExpressions: List<String> = emptyList(),
)

@Serializable
data class DailyReview(
    val day: Int,
    val progressZh: String,
    val mainProblemZh: String,
    val topMistakes: List<Correction>,
    val keyExpressions: List<Expression>,
    val tomorrowFocusZh: String,
)

data class TrainingMetrics(
    val listeningCorrect: Int = 0,
    val listeningTotal: Int = 0,
    val speakingMillis: Long = 0,
    val responseDelayMedianMs: Long = 0,
    val averageWordsPerTurn: Double = 0.0,
    val importantCorrections: Int = 0,
    val userTurnCount: Int = 0,
    val targetExpressionsUsed: Int = 0,
    val retellingWordCount: Int = 0,
) {
    val listeningAccuracy: Double get() = if (listeningTotal == 0) 0.0 else listeningCorrect.toDouble() / listeningTotal
    val importantCorrectionRate: Double get() = if (userTurnCount == 0) 0.0 else importantCorrections.toDouble() / userTurnCount
}

data class TrainingSession(
    val day: Int,
    val topic: String,
    val difficulty: Int,
    val status: TrainingStatus,
    val currentStep: TrainingStep,
    val metrics: TrainingMetrics = TrainingMetrics(),
)

data class LearningProgress(
    val currentDay: Int = 1,
    val completedDays: Int = 0,
    val totalSpeakingMillis: Long = 0,
    val expressionCount: Int = 0,
    val dueExpressionCount: Int = 0,
    val activeSession: TrainingSession? = null,
)

sealed interface AppError {
    data object NetworkUnavailable : AppError
    data object AiUnavailable : AppError
    data object InvalidAiResponse : AppError
    data object Unauthorized : AppError
    data object RateLimited : AppError
    data object SpeechUnavailable : AppError
    data object MicrophonePermissionDenied : AppError
    data object DatabaseError : AppError
    data class Unknown(val message: String = "") : AppError
}

data class DailyLessonRequest(val day: Int, val topic: String, val difficulty: Int)
data class ConversationContext(
    val day: Int,
    val topic: String,
    val difficulty: Int,
    val scenario: SpeakingScenario,
    val targetExpressions: List<String>,
    val turns: List<ConversationTurn>,
    val userMessage: String,
)
data class RetellingRequest(val task: String, val transcript: String)
data class DailyReviewRequest(
    val day: Int,
    val metrics: TrainingMetrics,
    val turns: List<ConversationTurn>,
    val retelling: RetellingFeedback?,
    val expressions: List<Expression>,
)

data class AppSettings(
    val trainingMode: TrainingMode = TrainingMode.STANDARD,
    val difficulty: Int = 1,
    val ttsRateOverride: Float? = null,
    val englishAccent: String = "en-US",
    val baseUrl: String = "https://api.deepseek.com",
    val model: String = "deepseek-v4-flash",
    val themeMode: String = "SYSTEM",
    val hasApiKey: Boolean = false,
)

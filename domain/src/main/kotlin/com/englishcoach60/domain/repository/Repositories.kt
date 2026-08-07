package com.englishcoach60.domain.repository

import com.englishcoach60.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AiRepository {
    suspend fun generateDailyLesson(request: DailyLessonRequest): DailyLesson
    suspend fun continueConversation(context: ConversationContext): ConversationReply
    suspend fun analyzeRetelling(request: RetellingRequest): RetellingFeedback
    suspend fun createDailyReview(request: DailyReviewRequest): DailyReview
    suspend fun lookupWord(query: String): WordLookup
    suspend fun testConnection(): Result<Unit>
}

interface TrainingRepository {
    fun observeProgress(): Flow<LearningProgress>
    suspend fun startOrResume(day: Int, topic: String, difficulty: Int): TrainingSession
    suspend fun updateStep(day: Int, step: TrainingStep)
    suspend fun updateDifficulty(day: Int, difficulty: Int)
    suspend fun updateMetrics(day: Int, metrics: TrainingMetrics)
    suspend fun saveTurn(turn: ConversationTurn)
    suspend fun loadTurns(day: Int): List<ConversationTurn>
    suspend fun loadMetrics(day: Int): TrainingMetrics?
    suspend fun completeDay(review: DailyReview, metrics: TrainingMetrics, expressions: List<Expression>)
    suspend fun reset()
}

interface LessonRepository {
    suspend fun getOrCreate(day: Int, topic: String, difficulty: Int): DailyLesson
    suspend fun regenerate(day: Int, topic: String, difficulty: Int): DailyLesson
}

interface ExpressionRepository {
    fun observeAll(): Flow<List<Expression>>
    fun observeDue(now: Long = System.currentTimeMillis()): Flow<List<Expression>>
    suspend fun review(expression: String, rating: ReviewRating)
    suspend fun togglePinned(expression: String)
    suspend fun save(expression: Expression)
    suspend fun delete(expression: String)
}

interface SettingsRepository {
    fun observe(): Flow<AppSettings>
    suspend fun update(settings: AppSettings)
}

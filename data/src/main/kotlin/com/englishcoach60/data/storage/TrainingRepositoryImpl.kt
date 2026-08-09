package com.englishcoach60.data.storage

import com.englishcoach60.database.*
import com.englishcoach60.domain.model.*
import com.englishcoach60.domain.repository.TrainingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import com.englishcoach60.domain.training.DifficultyPolicy

class TrainingRepositoryImpl(
    private val dao: CoachDao,
    private val settings: SettingsRepositoryImpl,
    private val json: Json,
) : TrainingRepository {
    override fun observeProgress(): Flow<LearningProgress> = combine(
        dao.observeTrainingDays(), dao.observeExpressions(), dao.observeAppProgress(),
    ) { days, expressions, progress ->
        val completed = days.filter { it.status == TrainingStatus.COMPLETED.name }
        val currentDay = progress?.currentDay ?: ((completed.maxOfOrNull { it.day } ?: 0) + 1).coerceAtMost(60)
        LearningProgress(
            currentDay = currentDay,
            completedDays = completed.size,
            totalSpeakingMillis = completed.sumOf { it.speakingMillis },
            expressionCount = expressions.size,
            dueExpressionCount = expressions.count { it.nextReviewAt <= System.currentTimeMillis() },
            activeSession = days.firstOrNull { it.status == TrainingStatus.IN_PROGRESS.name }?.toDomain(),
        )
    }

    override suspend fun startOrResume(day: Int, topic: String, difficulty: Int): TrainingSession {
        val existing = dao.trainingDay(day)
        if (existing != null) return existing.toDomain()
        val entity = TrainingDayEntity(day, topic, difficulty, TrainingStatus.IN_PROGRESS.name, TrainingStep.RECALL.name, System.currentTimeMillis())
        dao.upsertTrainingDay(entity)
        return entity.toDomain()
    }

    override suspend fun updateStep(day: Int, step: TrainingStep) = dao.updateStep(day, step.name)
    override suspend fun updateDifficulty(day: Int, difficulty: Int) = dao.updateDifficulty(day, difficulty.coerceIn(1, 4))
    override suspend fun resetForDifficulty(day: Int, difficulty: Int, step: TrainingStep) =
        dao.resetTrainingForDifficulty(day, difficulty.coerceIn(1, 4), step.name)
    override suspend fun updateMetrics(day: Int, metrics: TrainingMetrics) {
        dao.trainingDay(day)?.let { dao.upsertTrainingDay(metrics.applyTo(it)) }
    }
    override suspend fun saveTurn(turn: ConversationTurn) = dao.upsertTurn(turn.toEntity())
    override suspend fun loadTurns(day: Int) = dao.turns(day).map { it.toDomain() }
    override suspend fun loadMetrics(day: Int): TrainingMetrics? = dao.trainingDay(day)
        ?.takeIf { it.status == TrainingStatus.COMPLETED.name }
        ?.toDomain()
        ?.metrics

    override suspend fun completeDay(review: DailyReview, metrics: TrainingMetrics, expressions: List<Expression>) {
        val day = dao.trainingDay(review.day) ?: error("Training day not started")
        dao.completeTrainingDay(
            metrics.applyTo(day, completed = true),
            DailyReviewEntity(review.day, json.encodeToString(DailyReview.serializer(), review), System.currentTimeMillis()),
            expressions.map { it.toEntity() },
        )
        settings.setCurrentDay((review.day + 1).coerceAtMost(60))
        if (review.day % 3 == 0) {
            val recent = dao.recentCompletedDays(3).map { it.toDomain().metrics }
            val current = settings.observe().first()
            val nextDifficulty = DifficultyPolicy.next(current.difficulty, recent)
            if (nextDifficulty != current.difficulty) settings.update(current.copy(difficulty = nextDifficulty))
        }
    }

    override suspend fun reset() { dao.resetAll(); settings.reset() }
}

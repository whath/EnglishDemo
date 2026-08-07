package com.englishcoach60.data.storage

import com.englishcoach60.database.*
import com.englishcoach60.domain.model.*
import com.englishcoach60.domain.repository.ExpressionRepository
import com.englishcoach60.domain.training.ReviewScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExpressionRepositoryImpl(private val dao: CoachDao) : ExpressionRepository {
    override fun observeAll(): Flow<List<Expression>> = dao.observeExpressions().map { list -> list.map { it.toDomain() } }
    override fun observeDue(now: Long): Flow<List<Expression>> = dao.observeDueExpressions(now).map { list -> list.map { it.toDomain() } }
    override suspend fun review(expression: String, rating: ReviewRating) {
        val normalized = normalize(expression)
        val item = dao.expression(normalized) ?: return
        val schedule = ReviewScheduler.schedule(item.intervalDays, item.masteryLevel, rating)
        val now = System.currentTimeMillis()
        dao.updateExpression(item.copy(lastReviewedAt = now, nextReviewAt = now + schedule.intervalDays * 86_400_000L,
            intervalDays = schedule.intervalDays, masteryLevel = schedule.mastery, reviewCount = item.reviewCount + 1,
            mistakeCount = item.mistakeCount + if (rating == ReviewRating.AGAIN) 1 else 0))
        dao.insertExpressionReview(ExpressionReviewEntity(expressionId = item.id, rating = rating.name, reviewedAt = now))
    }
    override suspend fun togglePinned(expression: String) { dao.expression(normalize(expression))?.let { dao.updateExpression(it.copy(pinned = !it.pinned)) } }
    override suspend fun save(expression: Expression) {
        val candidate = expression.toEntity()
        val existing = dao.expression(candidate.normalized)
        if (existing == null) dao.insertExpression(candidate)
        else dao.updateExpression(existing.copy(
            meaningZh = candidate.meaningZh.ifBlank { existing.meaningZh },
            example = candidate.example.ifBlank { existing.example },
            sourceType = candidate.sourceType,
        ))
    }
    override suspend fun delete(expression: String) = dao.deleteExpression(normalize(expression))
    private fun normalize(value: String) = value.trim().lowercase().replace(Regex("\\s+"), " ")
}

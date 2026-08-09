package com.englishcoach60.data.storage

import com.englishcoach60.database.ConversationTurnEntity
import com.englishcoach60.database.ExpressionEntity
import com.englishcoach60.database.TrainingDayEntity
import com.englishcoach60.domain.model.ConversationTurn
import com.englishcoach60.domain.model.Correction
import com.englishcoach60.domain.model.CorrectionType
import com.englishcoach60.domain.model.Expression
import com.englishcoach60.domain.model.SourceType
import com.englishcoach60.domain.model.TrainingMetrics
import com.englishcoach60.domain.model.TrainingSession
import com.englishcoach60.domain.model.TrainingStatus
import com.englishcoach60.domain.model.TrainingStep

fun ExpressionEntity.toDomain() = Expression(expression, meaningZh, example, sourceDay, SourceType.valueOf(sourceType), masteryLevel, pinned, nextReviewAt, intervalDays)
fun Expression.toEntity(now: Long = System.currentTimeMillis()) = ExpressionEntity(
    expression = expression.trim(), normalized = expression.trim().lowercase().replace(Regex("\\s+"), " "), meaningZh = meaningZh,
    example = example, sourceDay = sourceDay, sourceType = sourceType.name, createdAt = now,
    nextReviewAt = if (nextReviewAt == 0L) now else nextReviewAt, intervalDays = intervalDays, masteryLevel = mastery, pinned = pinned,
)
fun TrainingDayEntity.toDomain() = TrainingSession(day, topic, difficulty, TrainingStatus.valueOf(status), TrainingStep.valueOf(currentStep),
    TrainingMetrics(listeningCorrect, listeningTotal, speakingMillis, responseDelayMedianMs, averageWordsPerTurn, importantCorrections, userTurnCount, targetExpressionsUsed, retellingWordCount))
fun ConversationTurnEntity.toDomain() = ConversationTurn(id, day, turnIndex, role, text,
    if (correctionType == CorrectionType.NONE.name) null else Correction(CorrectionType.valueOf(correctionType), text, correctedText, correctionExplanationZh),
    betterExpression, responseDelayMs, speechDurationMs)
fun ConversationTurn.toEntity() = ConversationTurnEntity(id, day, turnIndex, role, text, correction?.type?.name ?: CorrectionType.NONE.name,
    correction?.corrected.orEmpty(), correction?.explanationZh.orEmpty(), betterExpression, responseDelayMs, speechDurationMs, System.currentTimeMillis())
fun TrainingMetrics.applyTo(entity: TrainingDayEntity, completed: Boolean = false) = entity.copy(
    status = if (completed) TrainingStatus.COMPLETED.name else entity.status,
    completedAt = if (completed) System.currentTimeMillis() else entity.completedAt,
    listeningCorrect = listeningCorrect, listeningTotal = listeningTotal, speakingMillis = speakingMillis,
    responseDelayMedianMs = responseDelayMedianMs, averageWordsPerTurn = averageWordsPerTurn,
    importantCorrections = importantCorrections, userTurnCount = userTurnCount,
    targetExpressionsUsed = targetExpressionsUsed, retellingWordCount = retellingWordCount,
)

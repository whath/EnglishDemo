package com.englishcoach60.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CoachDao {
    @Query("SELECT * FROM training_days ORDER BY day")
    fun observeTrainingDays(): Flow<List<TrainingDayEntity>>

    @Query("SELECT * FROM training_days WHERE day = :day")
    suspend fun trainingDay(day: Int): TrainingDayEntity?

    @Query("SELECT * FROM training_days WHERE status = 'COMPLETED' ORDER BY day DESC LIMIT :limit")
    suspend fun recentCompletedDays(limit: Int): List<TrainingDayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrainingDay(day: TrainingDayEntity)

    @Query("UPDATE training_days SET currentStep = :step, status = 'IN_PROGRESS' WHERE day = :day")
    suspend fun updateStep(day: Int, step: String)

    @Query("UPDATE training_days SET difficulty = :difficulty WHERE day = :day")
    suspend fun updateDifficulty(day: Int, difficulty: Int)

    @Query("SELECT * FROM lessons WHERE day = :day")
    suspend fun lesson(day: Int): LessonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLesson(lesson: LessonEntity)

    @Query("DELETE FROM lessons WHERE day = :day")
    suspend fun deleteLesson(day: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTurn(turn: ConversationTurnEntity)

    @Query("SELECT * FROM conversation_turns WHERE day = :day ORDER BY turnIndex")
    suspend fun turns(day: Int): List<ConversationTurnEntity>

    @Query("SELECT * FROM expressions ORDER BY pinned DESC, nextReviewAt, expression")
    fun observeExpressions(): Flow<List<ExpressionEntity>>

    @Query("SELECT * FROM expressions WHERE nextReviewAt <= :now ORDER BY nextReviewAt LIMIT :limit")
    fun observeDueExpressions(now: Long, limit: Int = 5): Flow<List<ExpressionEntity>>

    @Query("SELECT * FROM expressions WHERE normalized = :normalized LIMIT 1")
    suspend fun expression(normalized: String): ExpressionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExpression(expression: ExpressionEntity): Long

    @Update
    suspend fun updateExpression(expression: ExpressionEntity)

    @Insert
    suspend fun insertExpressionReview(review: ExpressionReviewEntity)

    @Query("DELETE FROM expressions WHERE normalized = :normalized")
    suspend fun deleteExpression(normalized: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReview(review: DailyReviewEntity)

    @Query("SELECT * FROM app_progress WHERE id = 0")
    fun observeAppProgress(): Flow<AppProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppProgress(progress: AppProgressEntity)

    @Transaction
    suspend fun completeTrainingDay(
        completedDay: TrainingDayEntity,
        review: DailyReviewEntity,
        expressions: List<ExpressionEntity>,
    ) {
        upsertReview(review)
        expressions.forEach { candidate ->
            val existing = expression(candidate.normalized)
            if (existing == null) insertExpression(candidate)
            else updateExpression(existing.copy(
                meaningZh = candidate.meaningZh.ifBlank { existing.meaningZh },
                example = candidate.example.ifBlank { existing.example },
                sourceDay = candidate.sourceDay,
                sourceType = candidate.sourceType,
                mistakeCount = existing.mistakeCount + if (candidate.sourceType == "CORRECTION") 1 else 0,
            ))
        }
        upsertTrainingDay(completedDay)
        upsertAppProgress(AppProgressEntity(currentDay = (completedDay.day + 1).coerceAtMost(60)))
    }

    @Query("DELETE FROM conversation_turns") suspend fun clearTurns()
    @Query("DELETE FROM expression_reviews") suspend fun clearExpressionReviews()
    @Query("DELETE FROM expressions") suspend fun clearExpressions()
    @Query("DELETE FROM daily_reviews") suspend fun clearReviews()
    @Query("DELETE FROM lessons") suspend fun clearLessons()
    @Query("DELETE FROM training_days") suspend fun clearDays()
    @Query("DELETE FROM app_progress") suspend fun clearAppProgress()

    @Transaction
    suspend fun resetAll() {
        clearTurns(); clearExpressionReviews(); clearExpressions(); clearReviews(); clearLessons(); clearDays(); clearAppProgress()
    }
}

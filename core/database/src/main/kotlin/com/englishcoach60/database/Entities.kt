package com.englishcoach60.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "training_days")
data class TrainingDayEntity(
    @PrimaryKey val day: Int,
    val topic: String,
    val difficulty: Int,
    val status: String,
    val currentStep: String,
    val startedAt: Long,
    val completedAt: Long? = null,
    val listeningCorrect: Int = 0,
    val listeningTotal: Int = 0,
    val speakingMillis: Long = 0,
    val responseDelayMedianMs: Long = 0,
    val averageWordsPerTurn: Double = 0.0,
    val importantCorrections: Int = 0,
    val userTurnCount: Int = 0,
    val targetExpressionsUsed: Int = 0,
    val retellingWordCount: Int = 0,
)

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val day: Int,
    @ColumnInfo(defaultValue = "1") val difficulty: Int = 1,
    @ColumnInfo(defaultValue = "0") val contentVersion: Int = 0,
    val title: String,
    val objectiveZh: String,
    val listeningText: String,
    val translationZh: String,
    val expressionsJson: String,
    val questionsJson: String,
    val scenarioJson: String,
    val retellingPrompt: String,
    val createdAt: Long,
)

@Entity(tableName = "conversation_turns", indices = [Index(value = ["day", "turnIndex"], unique = true)])
data class ConversationTurnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Int,
    val turnIndex: Int,
    val role: String,
    val text: String,
    val correctionType: String,
    val correctedText: String,
    val correctionExplanationZh: String,
    val betterExpression: String,
    val responseDelayMs: Long,
    val speechDurationMs: Long,
    val createdAt: Long,
)

@Entity(tableName = "expressions", indices = [Index(value = ["normalized"], unique = true)])
data class ExpressionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expression: String,
    val normalized: String,
    val meaningZh: String,
    val example: String,
    val sourceDay: Int,
    val sourceType: String,
    val createdAt: Long,
    val lastReviewedAt: Long? = null,
    val nextReviewAt: Long,
    val intervalDays: Int = 0,
    val reviewCount: Int = 0,
    val mistakeCount: Int = 0,
    val masteryLevel: Int = 0,
    val pinned: Boolean = false,
)

@Entity(tableName = "expression_reviews")
data class ExpressionReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expressionId: Long,
    val rating: String,
    val reviewedAt: Long,
)

@Entity(tableName = "daily_reviews")
data class DailyReviewEntity(
    @PrimaryKey val day: Int,
    val reviewJson: String,
    val createdAt: Long,
)

@Entity(tableName = "app_progress")
data class AppProgressEntity(
    @PrimaryKey val id: Int = 0,
    val currentDay: Int = 1,
)

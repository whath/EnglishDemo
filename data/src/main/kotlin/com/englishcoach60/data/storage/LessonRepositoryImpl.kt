package com.englishcoach60.data.storage

import com.englishcoach60.database.*
import com.englishcoach60.domain.model.*
import com.englishcoach60.domain.repository.*
import kotlinx.serialization.json.Json

class LessonRepositoryImpl(private val dao: CoachDao, private val ai: AiRepository, private val json: Json) : LessonRepository {
    override suspend fun getOrCreate(day: Int, topic: String, difficulty: Int): DailyLesson {
        val cached = dao.lesson(day)
        return if (
            cached == null ||
            cached.difficulty != difficulty.coerceIn(1, 4) ||
            cached.contentVersion != CURRENT_LESSON_CONTENT_VERSION
        ) {
            create(day, topic, difficulty)
        } else {
            cached.toDomain()
        }
    }
    override suspend fun regenerate(day: Int, topic: String, difficulty: Int): DailyLesson {
        return create(day, topic, difficulty)
    }
    private suspend fun create(day: Int, topic: String, difficulty: Int): DailyLesson {
        val lesson = ai.generateDailyLesson(DailyLessonRequest(day, topic, difficulty))
        dao.upsertLesson(lesson.toEntity(json))
        return lesson
    }
}

private fun LessonEntity.toDomain(): DailyLesson {
    val json = Json { ignoreUnknownKeys = true }
    return DailyLesson(
        day,
        title,
        objectiveZh,
        listeningText,
        translationZh,
        json.decodeFromString(expressionsJson),
        json.decodeFromString(questionsJson),
        json.decodeFromString(scenarioJson),
        retellingPrompt,
        difficulty,
    )
}
private fun DailyLesson.toEntity(json: Json) = LessonEntity(
    day,
    difficulty,
    CURRENT_LESSON_CONTENT_VERSION,
    title,
    objectiveZh,
    listeningText,
    translationZh,
    json.encodeToString(expressions),
    json.encodeToString(questions),
    json.encodeToString(speakingScenario),
    retellingPrompt,
    System.currentTimeMillis(),
)

private const val CURRENT_LESSON_CONTENT_VERSION = 1

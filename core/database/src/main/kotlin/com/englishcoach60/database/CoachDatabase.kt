package com.englishcoach60.database

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [
        TrainingDayEntity::class,
        LessonEntity::class,
        ConversationTurnEntity::class,
        ExpressionEntity::class,
        ExpressionReviewEntity::class,
        DailyReviewEntity::class,
        AppProgressEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class CoachDatabase : RoomDatabase() {
    abstract fun coachDao(): CoachDao

    companion object {
        fun create(context: Context): CoachDatabase = Room.databaseBuilder<CoachDatabase>(
            context = context.applicationContext,
            name = "english-coach-60.db",
        ).setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
}

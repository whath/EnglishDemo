package com.englishcoach60.data.di

import android.content.Context
import com.englishcoach60.data.ai.AiRepositoryImpl
import com.englishcoach60.data.ai.ScriptedConversationProvider
import com.englishcoach60.data.storage.ExpressionRepositoryImpl
import com.englishcoach60.data.storage.LessonRepositoryImpl
import com.englishcoach60.data.storage.SettingsRepositoryImpl
import com.englishcoach60.data.storage.TrainingRepositoryImpl
import com.englishcoach60.database.CoachDao
import com.englishcoach60.database.CoachDatabase
import com.englishcoach60.domain.repository.AiRepository
import com.englishcoach60.domain.repository.ExpressionRepository
import com.englishcoach60.domain.repository.LessonRepository
import com.englishcoach60.domain.repository.SettingsRepository
import com.englishcoach60.domain.repository.TrainingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.InstallIn
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton fun database(@ApplicationContext context: Context) = CoachDatabase.create(context)
    @Provides @Singleton fun dao(database: CoachDatabase) = database.coachDao()
    @Provides @Singleton fun json(): Json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }
    @Provides @Singleton fun settings(@ApplicationContext context: Context): SettingsRepositoryImpl = SettingsRepositoryImpl(context)
    @Provides @Singleton fun settingsContract(impl: SettingsRepositoryImpl): SettingsRepository = impl
    @Provides @Singleton fun scripted() = ScriptedConversationProvider()
    @Provides @Singleton fun ai(settings: SettingsRepository, json: Json, scripted: ScriptedConversationProvider): AiRepository = AiRepositoryImpl(settings, json, scripted)
    @Provides @Singleton fun lessons(dao: CoachDao, ai: AiRepository, json: Json): LessonRepository = LessonRepositoryImpl(dao, ai, json)
    @Provides @Singleton fun training(dao: CoachDao, settings: SettingsRepositoryImpl, json: Json): TrainingRepository = TrainingRepositoryImpl(dao, settings, json)
    @Provides @Singleton fun expressions(dao: CoachDao): ExpressionRepository = ExpressionRepositoryImpl(dao)
}

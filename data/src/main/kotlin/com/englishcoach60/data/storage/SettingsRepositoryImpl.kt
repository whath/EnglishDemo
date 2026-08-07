package com.englishcoach60.data.storage

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.englishcoach60.data.BuildConfig
import com.englishcoach60.domain.model.*
import com.englishcoach60.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("coach_settings")

class SettingsRepositoryImpl(private val context: Context) : SettingsRepository {
    private object Keys {
        val mode = stringPreferencesKey("trainingMode")
        val difficulty = intPreferencesKey("difficulty")
        val rate = floatPreferencesKey("ttsRateOverride")
        val accent = stringPreferencesKey("englishAccent")
        val baseUrl = stringPreferencesKey("baseUrl")
        val model = stringPreferencesKey("model")
        val theme = stringPreferencesKey("themeMode")
        val currentDay = intPreferencesKey("currentTrainingDay")
    }

    override fun observe(): Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            trainingMode = runCatching { TrainingMode.valueOf(p[Keys.mode] ?: "STANDARD") }.getOrDefault(TrainingMode.STANDARD),
            difficulty = p[Keys.difficulty] ?: 1,
            ttsRateOverride = p[Keys.rate],
            englishAccent = p[Keys.accent] ?: "en-US",
            baseUrl = p[Keys.baseUrl] ?: "https://api.deepseek.com",
            model = p[Keys.model] ?: "deepseek-v4-flash",
            themeMode = p[Keys.theme] ?: "SYSTEM",
            hasApiKey = BuildConfig.DEEPSEEK_API_KEY.isNotBlank(),
        )
    }

    override suspend fun update(settings: AppSettings) {
        context.dataStore.edit { p ->
            p[Keys.mode] = settings.trainingMode.name
            p[Keys.difficulty] = settings.difficulty.coerceIn(1, 4)
            settings.ttsRateOverride?.let { p[Keys.rate] = it } ?: p.remove(Keys.rate)
            p[Keys.accent] = settings.englishAccent
            p[Keys.baseUrl] = settings.baseUrl
            p[Keys.model] = settings.model
            p[Keys.theme] = settings.themeMode
        }
    }

    suspend fun setCurrentDay(day: Int) { context.dataStore.edit { it[Keys.currentDay] = day.coerceIn(1, 60) } }
    suspend fun reset() { context.dataStore.edit { it.clear() } }
}

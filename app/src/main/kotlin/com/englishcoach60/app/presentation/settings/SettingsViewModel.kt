package com.englishcoach60.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishcoach60.app.presentation.toUserFacingAiMessage
import com.englishcoach60.domain.model.AppSettings
import com.englishcoach60.domain.repository.AiRepository
import com.englishcoach60.domain.repository.SettingsRepository
import com.englishcoach60.domain.repository.TrainingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(val settings: AppSettings = AppSettings(), val testing: Boolean = false, val connectionMessage: String? = null, val resetComplete: Boolean = false)

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repository: SettingsRepository, private val ai: AiRepository, private val training: TrainingRepository) : ViewModel() {
    private val transient = MutableStateFlow(SettingsUiState())
    val state = combine(repository.observe(), transient) { settings, temp -> temp.copy(settings = settings) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())
    fun update(transform: (AppSettings) -> AppSettings) = viewModelScope.launch { repository.update(transform(state.value.settings)) }
    fun testConnection() = viewModelScope.launch {
        transient.update { it.copy(testing = true, connectionMessage = null) }
        val result = ai.testConnection()
        transient.update {
            it.copy(
                testing = false,
                connectionMessage = if (result.isSuccess) {
                    "Connection successful. API key and model are valid."
                } else {
                    result.exceptionOrNull()?.toUserFacingAiMessage() ?: "Connection failed."
                },
            )
        }
    }
    fun reset() = viewModelScope.launch { training.reset(); transient.update { it.copy(resetComplete = true) } }
    fun clearMessage() = transient.update { it.copy(connectionMessage = null, resetComplete = false) }
}

package com.englishcoach60.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishcoach60.domain.model.*
import com.englishcoach60.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

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
        transient.update { it.copy(testing = false, connectionMessage = if (result.isSuccess) "Connection successful." else result.exceptionOrNull()?.message ?: "Connection failed.") }
    }
    fun reset() = viewModelScope.launch { training.reset(); transient.update { it.copy(resetComplete = true) } }
    fun clearMessage() = transient.update { it.copy(connectionMessage = null, resetComplete = false) }
}

package com.englishcoach60.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishcoach60.domain.model.AppSettings
import com.englishcoach60.domain.model.LearningProgress
import com.englishcoach60.domain.repository.SettingsRepository
import com.englishcoach60.domain.repository.TrainingRepository
import com.englishcoach60.domain.training.TrainingPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val progress: LearningProgress = LearningProgress(),
    val settings: AppSettings = AppSettings(),
) {
    val topic get() = TrainingPlan.topic(progress.currentDay)
    val isResume get() = progress.activeSession != null
    val programComplete get() = progress.completedDays >= 60
}

@HiltViewModel
class HomeViewModel @Inject constructor(training: TrainingRepository, settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<HomeUiState> = combine(training.observeProgress(), settings.observe()) { progress, config ->
        HomeUiState(false, progress, config)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}

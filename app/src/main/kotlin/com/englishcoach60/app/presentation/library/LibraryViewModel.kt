package com.englishcoach60.app.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishcoach60.domain.model.Expression
import com.englishcoach60.domain.model.ReviewRating
import com.englishcoach60.domain.repository.ExpressionRepository
import com.englishcoach60.speech.AndroidSpeechSynthesizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryFilter { DUE, ALL, PINNED }
data class LibraryUiState(val expressions: List<Expression> = emptyList(), val filter: LibraryFilter = LibraryFilter.DUE)

@HiltViewModel
class LibraryViewModel @Inject constructor(private val repository: ExpressionRepository, private val tts: AndroidSpeechSynthesizer) : ViewModel() {
    private val filter = MutableStateFlow(LibraryFilter.DUE)
    val state = combine(repository.observeAll(), filter) { list, selected -> LibraryUiState(list, selected) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())
    fun filter(value: LibraryFilter) { filter.value = value }
    fun review(item: Expression, rating: ReviewRating) = viewModelScope.launch { repository.review(item.expression, rating) }
    fun pin(item: Expression) = viewModelScope.launch { repository.togglePinned(item.expression) }
    fun delete(item: Expression) = viewModelScope.launch { repository.delete(item.expression) }
    fun speak(item: Expression) = tts.speak(item.expression)
}

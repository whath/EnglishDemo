package com.englishcoach60.app.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishcoach60.domain.model.Expression
import com.englishcoach60.domain.model.SourceType
import com.englishcoach60.domain.model.WordLookup
import com.englishcoach60.domain.repository.AiRepository
import com.englishcoach60.domain.repository.ExpressionRepository
import com.englishcoach60.speech.AndroidSpeechSynthesizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val result: WordLookup? = null,
    val suggestions: List<Expression> = emptyList(),
    val message: String? = null,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val expressionRepository: ExpressionRepository,
    private val tts: AndroidSpeechSynthesizer,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = mutableState.asStateFlow()
    private var allExpressions: List<Expression> = emptyList()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            expressionRepository.observeAll().collect { expressions ->
                allExpressions = expressions
                updateSuggestions(mutableState.value.query)
            }
        }
    }

    fun setQuery(value: String) {
        val clean = value.take(80)
        mutableState.update { it.copy(query = clean, message = null) }
        updateSuggestions(clean)
    }

    fun useSuggestion(expression: Expression) {
        setQuery(expression.expression)
        search()
    }

    fun search() {
        val query = mutableState.value.query.trim()
        if (query.isBlank()) {
            mutableState.update { it.copy(message = "Type a word or short expression first.") }
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, result = null, message = null) }
            runCatching { aiRepository.lookupWord(query) }
                .onSuccess { result -> mutableState.update { it.copy(loading = false, result = result) } }
                .onFailure { mutableState.update { it.copy(loading = false, message = "Couldn't look up that word. Check your connection and try again.") } }
        }
    }

    fun speakWord() = mutableState.value.result?.let { tts.speak(it.word, .85f) }
    fun speakExample() = mutableState.value.result?.let { tts.speak(it.example, .9f) }

    fun saveResult() {
        val result = mutableState.value.result ?: return
        viewModelScope.launch {
            expressionRepository.save(
                Expression(
                    expression = result.word,
                    meaningZh = result.meaningZh,
                    example = result.example,
                    sourceDay = 0,
                    sourceType = SourceType.MANUAL,
                    pinned = true,
                ),
            )
            mutableState.update { it.copy(message = "Saved to My Expressions.") }
        }
    }

    fun clearMessage() = mutableState.update { it.copy(message = null) }

    private fun updateSuggestions(query: String) {
        val normalized = query.trim().lowercase()
        val suggestions = if (normalized.isBlank()) allExpressions.take(4) else allExpressions
            .filter { it.expression.lowercase().contains(normalized) || it.meaningZh.contains(query.trim()) }
            .take(4)
        mutableState.update { it.copy(suggestions = suggestions) }
    }
}

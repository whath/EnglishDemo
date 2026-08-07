package com.englishcoach60.app.presentation.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishcoach60.domain.model.*
import com.englishcoach60.domain.repository.*
import com.englishcoach60.domain.training.TrainingMetricsCalculator
import com.englishcoach60.domain.training.TrainingPlan
import com.englishcoach60.speech.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SpeakingStatus { IDLE, LISTENING, RECOGNIZING, WAITING_FOR_AI, PLAYING_AI_SPEECH, ERROR }
data class RepeatComparison(val target: String, val recognized: String)

data class TrainingUiState(
    val loading: Boolean = true,
    val day: Int = 1,
    val step: TrainingStep = TrainingStep.RECALL,
    val lesson: DailyLesson? = null,
    val settings: AppSettings = AppSettings(),
    val dueExpressions: List<Expression> = emptyList(),
    val recallIndex: Int = 0,
    val revealRecall: Boolean = false,
    val transcriptVisible: Boolean = false,
    val listeningPlaying: Boolean = false,
    val listenedOnce: Boolean = false,
    val listeningSentenceIndex: Int = 0,
    val selectedAnswers: Map<Int, Int> = emptyMap(),
    val questionsChecked: Boolean = false,
    val repeatIndex: Int = 0,
    val repeatResults: List<String> = emptyList(),
    val repeatComparison: RepeatComparison? = null,
    val turns: List<ConversationTurn> = emptyList(),
    val quickFixes: List<Correction> = emptyList(),
    val quickFixIndex: Int = 0,
    val quickFixRecognized: String? = null,
    val speakingStatus: SpeakingStatus = SpeakingStatus.IDLE,
    val textInput: String = "",
    val retellingSegments: List<String> = emptyList(),
    val retellingFeedback: RetellingFeedback? = null,
    val review: DailyReview? = null,
    val metrics: TrainingMetrics = TrainingMetrics(),
    val dayOneBaseline: TrainingMetrics? = null,
    val error: String? = null,
) {
    val stepNumber get() = TrainingStep.entries.indexOf(step) + 1
    val repeatSentences get() = lesson?.listeningText.orEmpty().split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }.take(7)
    val quickFixActive get() = quickFixes.isNotEmpty()
}

@HiltViewModel
class TrainingViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository,
    private val lessonRepository: LessonRepository,
    private val expressionRepository: ExpressionRepository,
    private val settingsRepository: SettingsRepository,
    private val aiRepository: AiRepository,
    private val speech: AndroidSpeechController,
    private val tts: AndroidSpeechSynthesizer,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TrainingUiState())
    val state: StateFlow<TrainingUiState> = mutableState.asStateFlow()
    private var lastAiFinishedAt = 0L
    private var micStartedAt = 0L
    private val responseDelays = mutableListOf<Long>()
    private var targetUsage = 0
    private var aiRequestInFlight = false
    private var lastSubmittedText = ""
    private var lastSubmittedAt = 0L

    init {
        load()
        viewModelScope.launch {
            settingsRepository.observe().collect { settings ->
                tts.setLocale(settings.englishAccent)
                update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            speech.state.collect { speechState ->
                when (speechState) {
                    SpeechState.Idle -> update { it.copy(speakingStatus = SpeakingStatus.IDLE) }
                    SpeechState.Listening -> update { it.copy(speakingStatus = SpeakingStatus.LISTENING) }
                    SpeechState.Processing -> update { it.copy(speakingStatus = SpeakingStatus.RECOGNIZING) }
                    is SpeechState.Result -> { handleSpeechResult(speechState); speech.consumeResult() }
                    is SpeechState.Error -> update { it.copy(speakingStatus = SpeakingStatus.ERROR, error = speechState.message) }
                    SpeechState.Unavailable -> update { it.copy(speakingStatus = SpeakingStatus.ERROR, error = "Speech recognition is unavailable. Use the keyboard instead.") }
                }
            }
        }
    }

    fun retryLoad() = load()
    private fun load() = viewModelScope.launch {
        update { it.copy(loading = true, error = null) }
        runCatching {
            val progress = trainingRepository.observeProgress().first()
            val settings = settingsRepository.observe().first()
            tts.setLocale(settings.englishAccent)
            val day = progress.activeSession?.day ?: progress.currentDay
            val session = trainingRepository.startOrResume(day, TrainingPlan.topic(day), settings.difficulty)
            val difficultyChanged = session.difficulty != settings.difficulty
            if (difficultyChanged) trainingRepository.updateDifficulty(day, settings.difficulty)
            val lesson = if (difficultyChanged && session.currentStep == TrainingStep.RECALL) {
                lessonRepository.regenerate(day, session.topic, settings.difficulty)
            } else {
                lessonRepository.getOrCreate(day, session.topic, settings.difficulty)
            }
            val due = expressionRepository.observeDue().first().take(5)
            val turns = trainingRepository.loadTurns(day)
            val baseline = if (day == 60) trainingRepository.loadMetrics(1) else null
            update { it.copy(loading = false, day = day, step = session.currentStep, lesson = lesson, settings = settings, dueExpressions = due, turns = turns, dayOneBaseline = baseline) }
            if (session.currentStep == TrainingStep.SPEAKING && turns.isEmpty()) addOpeningLine(lesson)
            if (session.currentStep == TrainingStep.REVIEW) createReview()
        }.onFailure { error -> update { it.copy(loading = false, error = friendlyError(error)) } }
    }

    fun revealRecall() = update { it.copy(revealRecall = true) }
    fun rateRecall(rating: ReviewRating) = viewModelScope.launch {
        val item = state.value.dueExpressions.getOrNull(state.value.recallIndex)
        if (item != null) expressionRepository.review(item.expression, rating)
        val next = state.value.recallIndex + 1
        if (next >= state.value.dueExpressions.size) moveTo(TrainingStep.LISTENING)
        else update { it.copy(recallIndex = next, revealRecall = false) }
    }
    fun skipEmptyRecall() = viewModelScope.launch { moveTo(TrainingStep.LISTENING) }

    fun setDifficulty(level: Int) = viewModelScope.launch {
        val difficulty = level.coerceIn(1, 4)
        if (difficulty == state.value.settings.difficulty) return@launch
        val updatedSettings = state.value.settings.copy(difficulty = difficulty)
        settingsRepository.update(updatedSettings)
        trainingRepository.updateDifficulty(state.value.day, difficulty)
        update { it.copy(settings = updatedSettings) }
        if (state.value.step == TrainingStep.RECALL) {
            state.value.lesson ?: return@launch
            update { it.copy(loading = true, error = null) }
            runCatching { lessonRepository.regenerate(state.value.day, TrainingPlan.topic(state.value.day), difficulty) }
                .onSuccess { lesson -> update { it.copy(loading = false, lesson = lesson) } }
                .onFailure { error -> update { it.copy(loading = false, error = friendlyError(error)) } }
        }
    }

    fun playListening(replay: Boolean = false) {
        if (replay) update { it.copy(listeningSentenceIndex = 0) }
        update { it.copy(listeningPlaying = true, listenedOnce = true) }
        playListeningSentence()
    }
    private fun playListeningSentence() {
        val current = state.value
        val chunks = current.lesson?.listeningText.orEmpty().split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
        if (!current.listeningPlaying || current.listeningSentenceIndex >= chunks.size) { update { it.copy(listeningPlaying = false) }; return }
        tts.onDone = { viewModelScope.launch { update { it.copy(listeningSentenceIndex = it.listeningSentenceIndex + 1) }; playListeningSentence() } }
        tts.speak(chunks[current.listeningSentenceIndex], speechRate(current))
    }
    fun stopListening() { tts.stop(); update { it.copy(listeningPlaying = false) } }
    fun toggleTranscript() = update { it.copy(transcriptVisible = !it.transcriptVisible) }
    fun setTtsRate(rate: Float) = viewModelScope.launch {
        val updated = state.value.settings.copy(ttsRateOverride = rate)
        settingsRepository.update(updated)
        update { it.copy(settings = updated) }
    }
    fun selectAnswer(question: Int, option: Int) = update { it.copy(selectedAnswers = it.selectedAnswers + (question to option)) }
    fun checkQuestions() {
        val s = state.value
        val correct = s.lesson?.questions?.mapIndexed { i, q -> s.selectedAnswers[i] == q.answerIndex }?.count { it } ?: 0
        update { it.copy(questionsChecked = true, transcriptVisible = true, metrics = it.metrics.copy(listeningCorrect = correct, listeningTotal = s.lesson?.questions?.size ?: 0)) }
    }
    fun finishListening() = viewModelScope.launch { trainingRepository.updateMetrics(state.value.day, state.value.metrics); moveTo(TrainingStep.REPEAT) }

    fun playRepeatSentence() {
        val sentence = state.value.repeatSentences.getOrNull(state.value.repeatIndex) ?: return
        tts.onDone = null; tts.speak(sentence, speechRate(state.value))
    }
    fun skipRepeatSentence() = advanceRepeat("")
    fun confirmRepeat() = advanceRepeat(state.value.repeatComparison?.recognized.orEmpty())
    private fun showRepeatComparison(recognized: String) {
        val target = state.value.repeatSentences.getOrNull(state.value.repeatIndex) ?: return
        update { it.copy(repeatComparison = RepeatComparison(target, recognized)) }
    }
    private fun advanceRepeat(recognized: String) {
        val results = state.value.repeatResults + recognized
        val next = state.value.repeatIndex + 1
        if (next >= state.value.repeatSentences.size) viewModelScope.launch { moveTo(TrainingStep.SPEAKING) }
        else update { it.copy(repeatIndex = next, repeatResults = results, repeatComparison = null) }
    }

    fun startMic() {
        if (state.value.speakingStatus in listOf(SpeakingStatus.WAITING_FOR_AI, SpeakingStatus.RECOGNIZING)) return
        tts.stop(); micStartedAt = System.currentTimeMillis()
        if (lastAiFinishedAt > 0) responseDelays += (micStartedAt - lastAiFinishedAt).coerceAtLeast(0)
        speech.start()
    }
    fun stopMic() = speech.stop()
    fun setTextInput(value: String) = update { it.copy(textInput = value) }
    fun submitText() {
        val text = state.value.textInput.trim()
        if (text.isNotBlank()) { update { it.copy(textInput = "") }; routeTranscript(text, 0) }
    }
    private fun handleSpeechResult(result: SpeechState.Result) = routeTranscript(result.text, result.durationMs)
    private fun routeTranscript(text: String, durationMs: Long) {
        when (state.value.step) {
            TrainingStep.REPEAT -> showRepeatComparison(text)
            TrainingStep.SPEAKING -> if (state.value.quickFixActive) {
                update { it.copy(quickFixRecognized = text, metrics = it.metrics.copy(speakingMillis = it.metrics.speakingMillis + durationMs)) }
            } else submitSpeaking(text, durationMs)
            TrainingStep.RETELLING -> update { it.copy(retellingSegments = it.retellingSegments + text, metrics = it.metrics.copy(speakingMillis = it.metrics.speakingMillis + durationMs)) }
            else -> Unit
        }
    }

    private fun submitSpeaking(text: String, durationMs: Long) {
        val normalized = text.trim().lowercase().replace(Regex("\\s+"), " ")
        val now = System.currentTimeMillis()
        if (normalized.isBlank() || aiRequestInFlight || (normalized == lastSubmittedText && now - lastSubmittedAt < 2_000)) return
        aiRequestInFlight = true
        lastSubmittedText = normalized
        lastSubmittedAt = now
        viewModelScope.launch {
        update { it.copy(speakingStatus = SpeakingStatus.WAITING_FOR_AI, error = null) }
        val lesson = state.value.lesson
        if (lesson == null) { aiRequestInFlight = false; return@launch }
        val user = ConversationTurn(day = state.value.day, turnIndex = state.value.turns.size, role = "user", text = text, speechDurationMs = durationMs,
            responseDelayMs = responseDelays.lastOrNull() ?: 0)
        trainingRepository.saveTurn(user)
        update { it.copy(turns = it.turns + user, metrics = it.metrics.copy(speakingMillis = it.metrics.speakingMillis + durationMs)) }
        runCatching {
            val reply = aiRepository.continueConversation(ConversationContext(state.value.day, lesson.title, state.value.settings.difficulty,
                lesson.speakingScenario, lesson.expressions.map { it.expression }, state.value.turns.takeLast(10), text))
            val correctedUser = user.copy(correction = reply.correction.takeIf { it.type != CorrectionType.NONE }, betterExpression = reply.betterExpression)
            val ai = ConversationTurn(day = state.value.day, turnIndex = state.value.turns.size, role = "ai", text = reply.replyEnglish)
            trainingRepository.saveTurn(correctedUser); trainingRepository.saveTurn(ai)
            targetUsage += reply.usedTargetExpressions.count { used -> lesson.expressions.any { it.expression.equals(used, true) } }
            update { it.copy(turns = it.turns.dropLast(1) + correctedUser + ai, speakingStatus = SpeakingStatus.PLAYING_AI_SPEECH) }
            tts.onDone = { lastAiFinishedAt = System.currentTimeMillis(); update { it.copy(speakingStatus = SpeakingStatus.IDLE) } }
            tts.speak(reply.replyEnglish, speechRate(state.value))
        }.onFailure { error -> update { it.copy(speakingStatus = SpeakingStatus.ERROR, error = friendlyError(error)) } }
        aiRequestInFlight = false
        }
    }

    fun finishSpeaking() = viewModelScope.launch {
        calculateMetrics()
        val fixes = state.value.turns.mapNotNull { it.correction }
            .filter { it.type == CorrectionType.IMPORTANT && it.corrected.isNotBlank() }
            .distinctBy { it.corrected.lowercase() }
            .take(2)
        if (fixes.isEmpty()) moveTo(TrainingStep.RETELLING)
        else update { it.copy(quickFixes = fixes, quickFixIndex = 0, quickFixRecognized = null) }
    }
    fun playQuickFix() {
        val correction = state.value.quickFixes.getOrNull(state.value.quickFixIndex) ?: return
        tts.onDone = null
        tts.speak(correction.corrected, speechRate(state.value))
    }
    fun nextQuickFix() = viewModelScope.launch {
        val next = state.value.quickFixIndex + 1
        if (next >= state.value.quickFixes.size) {
            update { it.copy(quickFixes = emptyList(), quickFixRecognized = null) }
            moveTo(TrainingStep.RETELLING)
        } else update { it.copy(quickFixIndex = next, quickFixRecognized = null) }
    }
    fun analyzeRetelling() = viewModelScope.launch {
        val transcript = state.value.retellingSegments.joinToString(" ")
        if (transcript.isBlank()) return@launch
        update { it.copy(loading = true, error = null) }
        runCatching { aiRepository.analyzeRetelling(RetellingRequest(state.value.lesson?.retellingPrompt.orEmpty(), transcript)) }
            .onSuccess { feedback -> update { it.copy(loading = false, retellingFeedback = feedback) } }
            .onFailure { error -> update { it.copy(loading = false, error = friendlyError(error)) } }
    }
    fun finishRetelling() = viewModelScope.launch { calculateMetrics(); moveTo(TrainingStep.REVIEW); createReview() }

    private suspend fun createReview() {
        if (state.value.review != null) return
        update { it.copy(loading = true) }
        runCatching {
            calculateMetrics()
            aiRepository.createDailyReview(DailyReviewRequest(state.value.day, state.value.metrics, state.value.turns,
                state.value.retellingFeedback, state.value.lesson?.expressions.orEmpty()))
        }.onSuccess { review -> update { it.copy(loading = false, review = review) } }
            .onFailure { error -> update { it.copy(loading = false, error = friendlyError(error)) } }
    }
    fun completeDay(onComplete: () -> Unit) = viewModelScope.launch {
        val review = state.value.review ?: return@launch
        update { it.copy(loading = true) }
        val correctionExpressions = state.value.turns.mapNotNull { it.correction }
            .filter { it.type == CorrectionType.IMPORTANT && it.corrected.isNotBlank() }
            .map { correction ->
                Expression(
                    expression = correction.corrected,
                    meaningZh = correction.explanationZh,
                    example = correction.corrected,
                    sourceDay = state.value.day,
                    sourceType = SourceType.CORRECTION,
                )
            }
        val expressions = (review.keyExpressions + correctionExpressions)
            .distinctBy { it.expression.trim().lowercase().replace(Regex("\\s+"), " ") }
        runCatching { trainingRepository.completeDay(review, state.value.metrics, expressions) }
            .onSuccess { onComplete() }
            .onFailure { error -> update { it.copy(loading = false, error = friendlyError(error)) } }
    }

    fun clearError() = update { it.copy(error = null, speakingStatus = SpeakingStatus.IDLE) }
    fun previousStep() = viewModelScope.launch {
        val previous = TrainingStep.entries.getOrNull(state.value.stepNumber - 2) ?: return@launch
        tts.stop()
        speech.cancel()
        moveTo(previous)
    }
    private suspend fun moveTo(step: TrainingStep) {
        trainingRepository.updateStep(state.value.day, step)
        update { it.copy(step = step, error = null) }
        val lesson = state.value.lesson
        if (step == TrainingStep.SPEAKING && state.value.turns.isEmpty() && lesson != null) addOpeningLine(lesson)
    }
    private suspend fun addOpeningLine(lesson: DailyLesson) {
        val turn = ConversationTurn(day = state.value.day, turnIndex = 0, role = "ai", text = lesson.speakingScenario.openingLine)
        trainingRepository.saveTurn(turn); update { it.copy(turns = listOf(turn)) }
        update { it.copy(speakingStatus = SpeakingStatus.PLAYING_AI_SPEECH) }
        tts.onDone = { lastAiFinishedAt = System.currentTimeMillis(); update { it.copy(speakingStatus = SpeakingStatus.IDLE) } }
        tts.speak(turn.text, speechRate(state.value))
    }
    private suspend fun calculateMetrics() {
        val s = state.value
        val metrics = TrainingMetricsCalculator.calculate(s.metrics.listeningCorrect, s.metrics.listeningTotal, s.metrics.speakingMillis,
            s.turns.filter { it.role == "user" }, responseDelays, targetUsage, s.retellingSegments.joinToString(" "))
        update { it.copy(metrics = metrics) }; trainingRepository.updateMetrics(s.day, metrics)
    }
    private fun update(block: (TrainingUiState) -> TrainingUiState) { mutableState.update(block) }
    private fun speechRate(state: TrainingUiState): Float =
        state.settings.ttsRateOverride ?: TrainingPlan.ttsRate(state.day, state.settings.difficulty)
    private fun friendlyError(error: Throwable) = when {
        error.message?.contains("401") == true -> "AI key was rejected. Check local.properties."
        error.message?.contains("429") == true -> "AI service is busy. Try again shortly."
        error.message?.contains("JSON", true) == true -> "AI response couldn't be read. Try again."
        else -> "Something went wrong. Your progress is saved. Try again."
    }
    override fun onCleared() { speech.cancel(); tts.stop() }
}

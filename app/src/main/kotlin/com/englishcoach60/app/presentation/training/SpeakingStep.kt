package com.englishcoach60.app.presentation.training

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.englishcoach60.domain.model.ConversationTurn
import com.englishcoach60.domain.model.CorrectionType

@Composable
fun SpeakingStep(state: TrainingUiState, viewModel: TrainingViewModel, startMic: () -> Unit) {
    if (state.quickFixActive) {
        QuickFixPanel(state, viewModel, startMic)
        return
    }
    val listState = rememberLazyListState()
    var keyboard by remember { mutableStateOf(false) }
    LaunchedEffect(state.turns.size) { if (state.turns.isNotEmpty()) listState.animateScrollToItem(state.turns.lastIndex) }
    Column(Modifier.fillMaxSize()) {
        state.lesson?.speakingScenario?.let { scenario ->
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
                    Text("YOUR ROLE · ${scenario.userRole}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(scenario.goal, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), state = listState, contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.turns, key = { "${it.turnIndex}-${it.role}" }) { MessageBubble(it) }
            if (state.speakingStatus == SpeakingStatus.WAITING_FOR_AI) item { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)); Text("Thinking of a short reply…") } }
        }
        Surface(shadowElevation = 10.dp, tonalElevation = 2.dp, shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().imePadding().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (keyboard) OutlinedTextField(state.textInput, viewModel::setTextInput, Modifier.fillMaxWidth(), placeholder = { Text("Type your answer") }, trailingIcon = {
                    IconButton(viewModel::submitText, enabled = state.textInput.isNotBlank()) { Icon(Icons.Outlined.Send, "Send") }
                })
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton({ keyboard = !keyboard }, Modifier.weight(1f)) { Icon(Icons.Outlined.Keyboard, null); Spacer(Modifier.width(6.dp)); Text(if (keyboard) "Hide keyboard" else "Use keyboard") }
                    MicButton(state.speakingStatus, startMic, viewModel::stopMic)
                    TextButton(viewModel::finishSpeaking, Modifier.weight(1f), enabled = state.turns.any { it.role == "user" }) { Text("Retelling →") }
                }
                Text(statusText(state.speakingStatus), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun QuickFixPanel(state: TrainingUiState, viewModel: TrainingViewModel, startMic: () -> Unit) {
    val correction = state.quickFixes.getOrNull(state.quickFixIndex) ?: return
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Quick Fix", style = MaterialTheme.typography.headlineMedium)
        Text("${state.quickFixIndex + 1} / ${state.quickFixes.size} important correction", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Better", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(12.dp))
                Text(correction.corrected, style = MaterialTheme.typography.headlineMedium)
                if (correction.explanationZh.isNotBlank()) Text(correction.explanationZh, Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.quickFixRecognized?.let { Text("You said: $it", Modifier.padding(top = 20.dp), style = MaterialTheme.typography.bodyLarge) }
            }
        }
        OutlinedButton(viewModel::playQuickFix, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Listen once") }
        if (state.quickFixRecognized == null) {
            MicButton(state.speakingStatus, startMic, viewModel::stopMic)
            Text("Say the corrected sentence once.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else Button(viewModel::nextQuickFix, Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(if (state.quickFixIndex + 1 == state.quickFixes.size) "Continue to Retelling" else "Next Fix") }
    }
}

@Composable private fun MessageBubble(turn: ConversationTurn) {
    val user = turn.role == "user"
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (user) Alignment.End else Alignment.Start) {
        Surface(color = if (user) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, contentColor = if (user) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.medium, modifier = Modifier.widthIn(max = 320.dp)) { Text(turn.text, Modifier.padding(horizontal = 16.dp, vertical = 12.dp), style = MaterialTheme.typography.bodyLarge) }
        turn.correction?.takeIf { it.type != CorrectionType.NONE }?.let { correction ->
            Card(Modifier.padding(top = 6.dp).widthIn(max = 320.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Better", style = MaterialTheme.typography.labelLarge)
                    Text(correction.corrected, style = MaterialTheme.typography.bodyLarge)
                    Text(correction.explanationZh, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
        if (turn.betterExpression.isNotBlank() && turn.correction == null) Text("Try: ${turn.betterExpression}", Modifier.padding(6.dp), color = MaterialTheme.colorScheme.primary)
    }
}

private fun statusText(status: SpeakingStatus) = when (status) {
    SpeakingStatus.IDLE -> "Tap the mic, then speak naturally."
    SpeakingStatus.LISTENING -> "Listening…"
    SpeakingStatus.RECOGNIZING -> "Recognizing your words…"
    SpeakingStatus.WAITING_FOR_AI -> "Waiting for your conversation partner…"
    SpeakingStatus.PLAYING_AI_SPEECH -> "Playing the reply… tap mic to interrupt."
    SpeakingStatus.ERROR -> "Try again or use the keyboard."
}

package com.englishcoach60.app.presentation.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.englishcoach60.domain.model.ConversationTurn

@Composable
fun SpeakingStep(state: TrainingUiState, viewModel: TrainingViewModel, startMic: () -> Unit) {
    if (state.quickFixActive) {
        QuickFixPanel(state, viewModel, startMic)
        return
    }
    val listState = rememberLazyListState()
    var keyboard by remember { mutableStateOf(false) }
    val latestUserTurnIndex = state.turns.lastOrNull { it.role == "user" }?.turnIndex
    LaunchedEffect(state.turns.size) {
        if (state.turns.isNotEmpty()) {
            val latestUserIndex = state.turns.indexOfLast { it.role == "user" }
            listState.animateScrollToItem(latestUserIndex.takeIf { it >= 0 } ?: state.turns.lastIndex)
        }
    }
    Column(Modifier.fillMaxSize()) {
        state.lesson?.speakingScenario?.let { scenario ->
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text("YOUR ROLE · ${scenario.userRole}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(scenario.goal, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), state = listState, contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.turns, key = { "${it.turnIndex}-${it.role}" }) { turn ->
                MessageBubble(
                    turn = turn,
                    feedbackPending = state.speakingStatus == SpeakingStatus.WAITING_FOR_AI &&
                        turn.role == "user" &&
                        turn.turnIndex == latestUserTurnIndex,
                )
            }
            if (state.speakingStatus == SpeakingStatus.WAITING_FOR_AI) item { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)); Text("Thinking of a short reply…") } }
        }
        Surface(shadowElevation = 10.dp, tonalElevation = 2.dp, shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (keyboard) OutlinedTextField(state.textInput, viewModel::setTextInput, Modifier.fillMaxWidth(), placeholder = { Text("Type your answer") }, trailingIcon = {
                    IconButton(viewModel::submitText, enabled = state.textInput.isNotBlank()) { Icon(Icons.AutoMirrored.Outlined.Send, "Send") }
                })
                MicButton(state.speakingStatus, startMic, viewModel::stopMic)
                Text(
                    speechActivityText(state, "Tap the mic, then speak naturally."),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { keyboard = !keyboard },
                        modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                    ) {
                        Icon(Icons.Outlined.Keyboard, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (keyboard) "Hide keyboard" else "Keyboard", maxLines = 1)
                    }
                    Button(
                        onClick = viewModel::finishSpeaking,
                        modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                        enabled = state.turns.any { it.role == "user" },
                    ) {
                        Text("Retelling", maxLines = 1)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp))
                    }
                }
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
            Text(
                speechActivityText(state, "Say the corrected sentence once."),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else Button(viewModel::nextQuickFix, Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(if (state.quickFixIndex + 1 == state.quickFixes.size) "Continue to Retelling" else "Next Fix") }
    }
}

@Composable
private fun MessageBubble(turn: ConversationTurn, feedbackPending: Boolean) {
    val user = turn.role == "user"
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (user) Alignment.End else Alignment.Start) {
        Surface(color = if (user) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, contentColor = if (user) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.medium, modifier = Modifier.widthIn(max = 320.dp)) { Text(turn.text, Modifier.padding(horizontal = 16.dp, vertical = 12.dp), style = MaterialTheme.typography.bodyLarge) }
        if (user) UserLanguageFeedback(turn, feedbackPending)
    }
}

@Composable
private fun UserLanguageFeedback(turn: ConversationTurn, pending: Boolean) {
    Card(
        Modifier.padding(top = 7.dp).widthIn(max = 320.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                pending -> MaterialTheme.colorScheme.surfaceVariant
                turn.correction != null -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            },
        ),
    ) {
        if (pending) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Checking grammar and a better expression…")
            }
        } else {
            Column(
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("GRAMMAR", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                val correction = turn.correction
                when {
                    correction != null -> {
                        if (correction.original.isNotBlank() &&
                            !correction.original.equals(correction.corrected, ignoreCase = true)
                        ) {
                            Text(
                                "${correction.original} → ${correction.corrected}",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        } else {
                            Text(correction.corrected, style = MaterialTheme.typography.bodyLarge)
                        }
                        if (correction.explanationZh.isNotBlank()) {
                            Text(correction.explanationZh, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    turn.betterExpression.isNotBlank() -> Text("No clear grammar error found.")
                    else -> Text("Grammar feedback was unavailable for this reply.")
                }
                HorizontalDivider(Modifier.padding(vertical = 2.dp))
                Text("BETTER EXPRESSION", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    turn.betterExpression.ifBlank { "A better-expression suggestion was unavailable for this reply." },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

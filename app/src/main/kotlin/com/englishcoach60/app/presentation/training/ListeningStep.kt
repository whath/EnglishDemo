package com.englishcoach60.app.presentation.training

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.englishcoach60.designsystem.*

@Composable
fun ListeningStep(state: TrainingUiState, viewModel: TrainingViewModel) {
    val lesson = state.lesson ?: return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SectionLabel("First, just listen")
        Text(lesson.title, style = MaterialTheme.typography.headlineMedium)
        Text(lesson.objectiveZh, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(
            Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
          Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = { viewModel.playListening(replay = true) }, modifier = Modifier.size(58.dp)) { Icon(Icons.Outlined.Replay, "Replay", Modifier.size(27.dp)) }
                FilledIconButton(onClick = { if (state.listeningPlaying) viewModel.stopListening() else viewModel.playListening() }, modifier = Modifier.size(78.dp)) {
                    Icon(if (state.listeningPlaying) Icons.Outlined.Stop else Icons.Filled.PlayArrow, if (state.listeningPlaying) "Stop" else "Play", Modifier.size(38.dp))
                }
                Column { FilterChip(selected = (state.settings.ttsRateOverride ?: com.englishcoach60.domain.training.TrainingPlan.ttsRate(state.day)) == .85f, onClick = { viewModel.setTtsRate(.85f) }, label = { Text("0.85x") }); FilterChip(selected = state.settings.ttsRateOverride == 1f, onClick = { viewModel.setTtsRate(1f) }, label = { Text("1.0x") }) }
            }
            Text(if (state.listeningPlaying) "Listening… sentence ${state.listeningSentenceIndex + 1}" else "Tap play and focus on the main idea.", color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
        TextButton(onClick = viewModel::toggleTranscript) { Text(if (state.transcriptVisible) "Hide Transcript" else "Show Transcript") }
        if (state.transcriptVisible) CoachCard(Modifier.fillMaxWidth()) {
            Text(lesson.listeningText, style = MaterialTheme.typography.bodyLarge)
            if (state.questionsChecked) { HorizontalDivider(); Text(lesson.translationZh, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (!state.listenedOnce) {
            Text("Questions will appear after you listen once.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
        SectionLabel("Check understanding")
        lesson.questions.forEachIndexed { index, question ->
            CoachCard(Modifier.fillMaxWidth()) {
                Text("${index + 1}. ${question.question}", style = MaterialTheme.typography.titleMedium)
                question.options.forEachIndexed { optionIndex, option ->
                    val selected = state.selectedAnswers[index] == optionIndex
                    val color = if (state.questionsChecked && optionIndex == question.answerIndex) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    Surface(onClick = { if (!state.questionsChecked) viewModel.selectAnswer(index, optionIndex) }, color = color, shape = MaterialTheme.shapes.small, border = ButtonDefaults.outlinedButtonBorder(enabled = true)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected, onClick = null); Text(option)
                        }
                    }
                }
            }
        }
        if (!state.questionsChecked) PrimaryCoachButton("Check Answers", viewModel::checkQuestions, Modifier.fillMaxWidth(), state.selectedAnswers.size == lesson.questions.size)
        else {
            Text("Listening accuracy: ${state.metrics.listeningCorrect} / ${state.metrics.listeningTotal}", style = MaterialTheme.typography.titleLarge)
            SectionLabel("5 key expressions")
            lesson.expressions.forEach { Text("${it.expression}  ·  ${it.meaningZh}", style = MaterialTheme.typography.bodyLarge) }
            PrimaryCoachButton("Continue", viewModel::finishListening, Modifier.fillMaxWidth())
        }
        }
    }
}

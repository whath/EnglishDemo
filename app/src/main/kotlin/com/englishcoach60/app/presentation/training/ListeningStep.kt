package com.englishcoach60.app.presentation.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.englishcoach60.designsystem.CoachCard
import com.englishcoach60.designsystem.PrimaryCoachButton
import com.englishcoach60.designsystem.SectionLabel
import com.englishcoach60.domain.training.TrainingPlan

@Composable
fun ListeningStep(state: TrainingUiState, viewModel: TrainingViewModel) {
    val lesson = state.lesson ?: return
    val coachRate = TrainingPlan.ttsRate(state.day, state.settings.difficulty)
    val selectedRate = state.settings.ttsRateOverride ?: coachRate
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SectionLabel("First, just listen")
        Text(lesson.title, style = MaterialTheme.typography.headlineMedium)
        Text(lesson.objectiveZh, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(
            Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
          Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = { viewModel.playListening(replay = true) }, modifier = Modifier.size(52.dp)) { Icon(Icons.Outlined.Replay, "Replay", Modifier.size(24.dp)) }
                Spacer(Modifier.width(28.dp))
                FilledIconButton(onClick = { if (state.listeningPlaying) viewModel.stopListening() else viewModel.playListening() }, modifier = Modifier.size(72.dp)) {
                    Icon(if (state.listeningPlaying) Icons.Outlined.Stop else Icons.Filled.PlayArrow, if (state.listeningPlaying) "Stop" else "Play", Modifier.size(34.dp))
                }
            }
            Text(if (state.listeningPlaying) "Listening… sentence ${state.listeningSentenceIndex + 1}" else "Tap play and focus on the main idea.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .12f))
            Text("Playback speed", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedRate == coachRate,
                    onClick = { viewModel.setTtsRate(coachRate) },
                    label = { Text("Coach") },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = selectedRate == .85f,
                    onClick = { viewModel.setTtsRate(.85f) },
                    label = { Text("0.85×") },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = selectedRate == 1f,
                    onClick = { viewModel.setTtsRate(1f) },
                    label = { Text("1.0×") },
                    modifier = Modifier.weight(1f),
                )
            }
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

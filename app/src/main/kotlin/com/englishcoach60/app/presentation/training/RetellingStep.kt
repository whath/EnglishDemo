package com.englishcoach60.app.presentation.training

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.englishcoach60.designsystem.*

@Composable
fun RetellingStep(state: TrainingUiState, viewModel: TrainingViewModel, startMic: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SectionLabel("Retelling")
        Text("Say it in your own words", style = MaterialTheme.typography.headlineMedium)
        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("YOUR PROMPT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(state.lesson?.retellingPrompt.orEmpty(), style = MaterialTheme.typography.titleLarge)
                Text("Record a few short segments — we'll bring them together.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        MicButton(state.speakingStatus, startMic, viewModel::stopMic)
        Text(if (state.speakingStatus == SpeakingStatus.LISTENING) "Listening…" else "${state.retellingSegments.size} segment(s) recorded")
        OutlinedTextField(state.textInput, viewModel::setTextInput, Modifier.fillMaxWidth(), label = { Text("Keyboard fallback") }, placeholder = { Text("Type one retelling segment") },
            trailingIcon = { TextButton(viewModel::submitText, enabled = state.textInput.isNotBlank()) { Text("Add") } })
        state.retellingSegments.forEachIndexed { index, segment ->
            Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primary) { Text("${index + 1}", Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium) }
                    Spacer(Modifier.width(12.dp)); Text(segment, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        if (state.retellingSegments.isNotEmpty() && state.retellingFeedback == null) PrimaryCoachButton("Analyze Retelling", viewModel::analyzeRetelling, Modifier.fillMaxWidth())
        state.retellingFeedback?.let { feedback ->
            CoachCard(Modifier.fillMaxWidth()) {
                SectionLabel("Coaching")
                Text(feedback.summaryZh, style = MaterialTheme.typography.bodyLarge)
                if (feedback.correctedVersion.isNotBlank()) { HorizontalDivider(); Text("A clearer version", style = MaterialTheme.typography.labelLarge); Text(feedback.correctedVersion) }
                feedback.topIssues.forEach { Text("${it.original} → ${it.better}\n${it.explanationZh}") }
            }
            PrimaryCoachButton("Daily Review", viewModel::finishRetelling, Modifier.fillMaxWidth())
        }
    }
}

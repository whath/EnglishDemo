package com.englishcoach60.app.presentation.training

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.englishcoach60.designsystem.*

@Composable
fun RepeatStep(state: TrainingUiState, viewModel: TrainingViewModel, startMic: () -> Unit) {
    val target = state.repeatSentences.getOrNull(state.repeatIndex)
    if (target == null) return
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SectionLabel(if (state.day <= 15) "Listen · pause · repeat" else "Listen · short delay · repeat")
        Text("Make the sentence yours", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text("${state.repeatIndex + 1} / ${state.repeatSentences.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        CoachCard(Modifier.fillMaxWidth().weight(1f)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(target, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    state.repeatComparison?.let { comparison ->
                        HorizontalDivider()
                        SectionLabel("Speech recognition match")
                        Text("Heard: ${comparison.recognized}", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                        Text("Compare the recognized words with the target. This is not a pronunciation score.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        OutlinedButton(viewModel::playRepeatSentence, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Icon(Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Listen") }
        if (state.repeatComparison == null) {
            MicButton(state.speakingStatus, startMic, viewModel::stopMic)
            Text(if (state.speakingStatus == SpeakingStatus.LISTENING) "Listening… tap to stop" else "Repeat when you're ready.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(viewModel::skipRepeatSentence) { Text("Skip this sentence") }
        } else PrimaryCoachButton("Next Sentence", viewModel::confirmRepeat, Modifier.fillMaxWidth())
    }
}

@Composable
fun MicButton(status: SpeakingStatus, start: () -> Unit, stop: () -> Unit) {
    val listening = status == SpeakingStatus.LISTENING
    val transition = rememberInfiniteTransition(label = "micPulse")
    val pulse = transition.animateFloat(
        initialValue = 1f,
        targetValue = if (listening) 1.14f else 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "micPulseScale",
    ).value
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(86.dp)) {
        if (listening) Surface(
            modifier = Modifier.size(72.dp).scale(pulse),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = .12f),
            content = {},
        )
        FilledIconButton(
            onClick = if (listening) stop else start,
            enabled = status !in listOf(SpeakingStatus.RECOGNIZING, SpeakingStatus.WAITING_FOR_AI),
            modifier = Modifier.size(68.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (listening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(if (listening) Icons.Outlined.Stop else Icons.Filled.Mic, if (listening) "Stop recording" else "Start microphone", Modifier.size(30.dp))
        }
    }
}

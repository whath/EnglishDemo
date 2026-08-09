package com.englishcoach60.app.presentation.training

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionLabel(if (state.day <= 15) "Listen · pause · repeat" else "Listen · short delay · repeat")
        Text("Make the sentence yours", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Two examples · both optional", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    "${state.repeatIndex + 1} / ${state.repeatSentences.size}",
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        CoachCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(target, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                state.repeatComparison?.let { comparison ->
                    HorizontalDivider()
                    SectionLabel("Speech recognition match")
                    Text("Heard: ${comparison.recognized}", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    Text("Compare the recognized words with the target. This is not a pronunciation score.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }
        OutlinedButton(viewModel::playRepeatSentence, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Icon(Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Listen") }
        if (state.repeatComparison == null) {
            MicButton(state.speakingStatus, startMic, viewModel::stopMic)
            Text(
                speechActivityText(state, "Repeat when you're ready."),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            TextButton(viewModel::skipRepeatSentence) { Text("Skip sentence") }
        } else PrimaryCoachButton("Next Sentence", viewModel::confirmRepeat, Modifier.fillMaxWidth())
        TextButton(viewModel::skipRepeatPractice) { Text("Skip Listen & Repeat") }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
fun MicButton(status: SpeakingStatus, start: () -> Unit, stop: () -> Unit) {
    val listening = status == SpeakingStatus.LISTENING
    val busy = status in listOf(
        SpeakingStatus.PREPARING,
        SpeakingStatus.RECOGNIZING,
        SpeakingStatus.WAITING_FOR_AI,
    )
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
            enabled = !busy,
            modifier = Modifier.size(68.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (listening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            ),
        ) {
            if (busy) {
                CircularProgressIndicator(
                    Modifier.size(28.dp),
                    color = LocalContentColor.current,
                    strokeWidth = 2.5.dp,
                )
            } else {
                Icon(if (listening) Icons.Outlined.Stop else Icons.Filled.Mic, if (listening) "Stop recording" else "Start microphone", Modifier.size(30.dp))
            }
        }
    }
}

internal fun speechActivityText(state: TrainingUiState, idleText: String): String = when (state.speakingStatus) {
    SpeakingStatus.IDLE -> idleText
    SpeakingStatus.PREPARING -> state.speechPreparationProgress?.let { progress ->
        if (progress < 100) "Downloading offline English model · $progress%" else "Loading offline English recognition…"
    } ?: "Preparing offline English recognition…"
    SpeakingStatus.LISTENING -> "Listening… tap to stop"
    SpeakingStatus.RECOGNIZING -> "Recognizing your words offline…"
    SpeakingStatus.WAITING_FOR_AI -> "Waiting for your conversation partner…"
    SpeakingStatus.PLAYING_AI_SPEECH -> "Playing the reply… tap mic to interrupt."
    SpeakingStatus.ERROR -> "Try again or use the keyboard."
}

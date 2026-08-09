package com.englishcoach60.app.presentation.training

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.englishcoach60.designsystem.CoachCard
import com.englishcoach60.designsystem.PrimaryCoachButton
import com.englishcoach60.designsystem.SectionLabel
import com.englishcoach60.domain.model.ReviewRating
import kotlin.math.abs

@Composable
fun RecallStep(
    state: TrainingUiState,
    onReveal: () -> Unit,
    onRate: (ReviewRating) -> Unit,
    onSkipEmpty: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SectionLabel("Warm up")
        Text("Recall before you look", style = MaterialTheme.typography.headlineMedium)
        if (state.dueExpressions.isEmpty()) {
            CoachCard(Modifier.fillMaxWidth()) {
                Text("No expressions are due yet.", style = MaterialTheme.typography.titleLarge)
                Text("Your saved expressions will appear here on future training days.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            PrimaryCoachButton("Start Listening", onSkipEmpty, Modifier.fillMaxWidth())
        } else {
            val item = state.dueExpressions[state.recallIndex]
            var dragOffset by remember(item.expression, state.revealRecall) { mutableFloatStateOf(0f) }
            val dragThreshold = with(LocalDensity.current) { 92.dp.toPx() }
            val maxDrag = with(LocalDensity.current) { 180.dp.toPx() }
            Text("${state.recallIndex + 1} of ${state.dueExpressions.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.fillMaxWidth().weight(1f)) {
                if (state.revealRecall && abs(dragOffset) > 12f) {
                    Surface(
                        modifier = Modifier.align(if (dragOffset > 0) Alignment.CenterStart else Alignment.CenterEnd),
                        shape = MaterialTheme.shapes.medium,
                        color = if (dragOffset > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Text(
                            if (dragOffset > 0) "GOOD" else "AGAIN",
                            Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            color = if (dragOffset > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                CoachCard(
                    Modifier.fillMaxSize().testTag("recall-card")
                        .graphicsLayer {
                            translationX = dragOffset
                            rotationZ = dragOffset / 55f
                            alpha = 1f - (abs(dragOffset) / (maxDrag * 3f)).coerceIn(0f, .16f)
                        }
                        .pointerInput(item.expression, state.revealRecall) {
                            detectHorizontalDragGestures(
                                onDragCancel = { dragOffset = 0f },
                                onDragEnd = {
                                    when {
                                        state.revealRecall && dragOffset >= dragThreshold -> onRate(ReviewRating.GOOD)
                                        state.revealRecall && dragOffset <= -dragThreshold -> onRate(ReviewRating.AGAIN)
                                    }
                                    dragOffset = 0f
                                },
                            ) { change, amount ->
                                if (state.revealRecall) {
                                    change.consume()
                                    dragOffset = (dragOffset + amount).coerceIn(-maxDrag, maxDrag)
                                }
                            }
                        },
                ) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(item.meaningZh, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                            Text("Say the English expression aloud.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (state.revealRecall) {
                                HorizontalDivider()
                                Text(item.expression, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                                Text(item.example, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
            if (!state.revealRecall) PrimaryCoachButton("Reveal", onReveal, Modifier.fillMaxWidth())
            else {
                Text("Swipe left: Again  ·  Swipe right: Good", Modifier.align(Alignment.CenterHorizontally), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ onRate(ReviewRating.AGAIN) }, Modifier.weight(1f).heightIn(min = 52.dp)) { Text("Again") }
                    OutlinedButton({ onRate(ReviewRating.HARD) }, Modifier.weight(1f).heightIn(min = 52.dp)) { Text("Hard") }
                    Button({ onRate(ReviewRating.GOOD) }, Modifier.weight(1f).heightIn(min = 52.dp)) { Text("Good") }
                }
            }
        }
    }
}

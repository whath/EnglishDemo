package com.englishcoach60.app.presentation.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.englishcoach60.designsystem.CoachCard
import com.englishcoach60.designsystem.PrimaryCoachButton
import com.englishcoach60.designsystem.SectionLabel
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ReviewStep(state: TrainingUiState, onComplete: () -> Unit) {
    val review = state.review ?: return
    val minutes = (state.metrics.speakingMillis / 60_000.0)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Card(
            Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("DAY ${state.day} COMPLETE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .72f))
                Text("You showed up.", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary)
                Text("You spoke ${if (minutes < 1) "under 1" else minutes.roundToInt()} min today.", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .86f))
            }
        }
        SectionLabel("Today's numbers")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReviewMetric("Listening", "${state.metrics.listeningCorrect}/${state.metrics.listeningTotal}", Modifier.weight(1f))
            ReviewMetric("Words / turn", String.format(Locale.US, "%.1f", state.metrics.averageWordsPerTurn), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReviewMetric("Response", if (state.metrics.responseDelayMedianMs == 0L) "—" else "${state.metrics.responseDelayMedianMs / 1000}s", Modifier.weight(1f))
            ReviewMetric("Important fixes", state.metrics.importantCorrections.toString(), Modifier.weight(1f))
        }
        ReviewMetric("Target expressions used", state.metrics.targetExpressionsUsed.toString(), Modifier.fillMaxWidth())
        if (state.day == 60 && state.dayOneBaseline != null) {
            DaySixtyComparison(state.dayOneBaseline, state.metrics)
        }
        CoachCard(Modifier.fillMaxWidth()) { SectionLabel("What improved"); Text(review.progressZh, style = MaterialTheme.typography.bodyLarge) }
        CoachCard(Modifier.fillMaxWidth()) {
            SectionLabel("What to fix"); Text(review.mainProblemZh, style = MaterialTheme.typography.bodyLarge)
            review.topMistakes.forEach { Text("${it.original} → ${it.corrected}") }
        }
        CoachCard(Modifier.fillMaxWidth()) {
            SectionLabel("Expressions")
            review.keyExpressions.forEach { Text(it.expression, style = MaterialTheme.typography.titleMedium); Text(it.meaningZh, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        CoachCard(Modifier.fillMaxWidth()) { SectionLabel("Tomorrow"); Text(review.tomorrowFocusZh, style = MaterialTheme.typography.bodyLarge) }
        PrimaryCoachButton("Finish Day", onComplete, Modifier.fillMaxWidth())
    }
}

@Composable
private fun DaySixtyComparison(dayOne: com.englishcoach60.domain.model.TrainingMetrics, daySixty: com.englishcoach60.domain.model.TrainingMetrics) {
    fun seconds(value: Long) = if (value == 0L) "—" else "${value / 1000}s"
    fun percent(value: Double) = "${(value * 100).roundToInt()}%"
    CoachCard(Modifier.fillMaxWidth()) {
        SectionLabel("Day 1 → Day 60")
        Text("Your real training metrics", style = MaterialTheme.typography.titleLarge)
        Text("Speaking: ${dayOne.speakingMillis / 60_000} min → ${daySixty.speakingMillis / 60_000} min")
        Text("Words / turn: ${String.format(Locale.US, "%.1f", dayOne.averageWordsPerTurn)} → ${String.format(Locale.US, "%.1f", daySixty.averageWordsPerTurn)}")
        Text("Response delay: ${seconds(dayOne.responseDelayMedianMs)} → ${seconds(daySixty.responseDelayMedianMs)}")
        Text("Important correction rate: ${percent(dayOne.importantCorrectionRate)} → ${percent(daySixty.importantCorrectionRate)}")
        Text("Listening accuracy: ${percent(dayOne.listeningAccuracy)} → ${percent(daySixty.listeningAccuracy)}")
        Text("Retelling words: ${dayOne.retellingWordCount} → ${daySixty.retellingWordCount}")
    }
}

@Composable private fun ReviewMetric(label: String, value: String, modifier: Modifier) {
    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

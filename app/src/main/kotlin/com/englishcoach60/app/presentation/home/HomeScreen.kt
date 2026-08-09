package com.englishcoach60.app.presentation.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onTraining: () -> Unit,
    onSearch: () -> Unit,
    onLibrary: () -> Unit,
    onSettings: () -> Unit,
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(42.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("60", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("English Coach", style = MaterialTheme.typography.titleMedium)
                    Text("A little better, every day", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onLibrary) { Icon(Icons.Outlined.AutoStories, "My Expressions") }
                IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Settings") }
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(if (state.programComplete) "YOUR JOURNEY" else "TODAY'S PRACTICE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(5.dp))
                    AnimatedContent(
                        targetState = state.progress.currentDay,
                        transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(160)) },
                        label = "dayNumber",
                    ) { day ->
                        Text(if (state.programComplete) "60 Days Complete" else "Day ${day.toString().padStart(2, '0')}", style = MaterialTheme.typography.displaySmall)
                    }
                }
                if (!state.programComplete || !state.settings.hasApiKey) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (!state.programComplete) {
                            Text(
                                "of 60",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (!state.settings.hasApiKey) {
                            AssistChip(
                                onClick = onSettings,
                                label = { Text("Demo") },
                                modifier = Modifier.heightIn(min = 32.dp),
                            )
                        }
                    }
                }
            }

            SearchEntry(onClick = onSearch)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
            ) {
                Column(Modifier.padding(24.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (state.programComplete) {
                        Text("TRAINING COMPLETE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .72f))
                        Text("You showed up for all 60 days.", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary)
                        Text("Your progress is built from real speaking, listening and expression practice.", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .78f), style = MaterialTheme.typography.bodyLarge)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .13f)) {
                                Icon(Icons.Outlined.LocalFireDepartment, null, Modifier.padding(9.dp), tint = MaterialTheme.colorScheme.onPrimary)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text("TODAY'S FOCUS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .72f))
                            Spacer(Modifier.weight(1f))
                            Text("~${trainingMinutes(state.settings.trainingMode.name)} min", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
                        }
                        Text(state.topic, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary)
                        Text(practicalGoal(state.progress.currentDay), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .8f), style = MaterialTheme.typography.bodyLarge)
                        Button(
                            onClick = onTraining,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(if (state.isResume) "Continue Training" else "Start Training", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(8.dp)); Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
                        }
                    }
                }
            }

            if (state.progress.dueExpressionCount > 0) {
                Surface(
                    onClick = onLibrary,
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Ready for review", style = MaterialTheme.typography.titleMedium)
                            Text("${state.progress.dueExpressionCount} expressions are due", style = MaterialTheme.typography.bodyMedium)
                        }
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Review")
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Your progress", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text("${state.progress.completedDays}/60 days", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(
                progress = { state.progress.completedDays / 60f },
                modifier = Modifier.fillMaxWidth().height(7.dp),
                trackColor = MaterialTheme.colorScheme.primaryContainer,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Speaking", "${state.progress.totalSpeakingMillis / 60_000}", "minutes", Modifier.weight(1f))
                MetricCard("Expressions", state.progress.expressionCount.toString(), "collected", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SearchEntry(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp,
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Look up a word", style = MaterialTheme.typography.titleMedium)
                Text("中文 → English · English → 中文", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Open dictionary", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, unit: String, modifier: Modifier) {
    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(5.dp))
                Text(unit, Modifier.padding(bottom = 3.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun trainingMinutes(mode: String) = when (mode) { "QUICK" -> 25; "INTENSIVE" -> 60; else -> 45 }

private fun practicalGoal(day: Int) = when (day) {
    1 -> "Present your background, responsibilities, and goals with supporting details."
    in 2..10 -> "Build a strong university-level foundation for connected communication."
    in 11..20 -> "Handle common travel and service situations."
    in 21..30 -> "Keep a simple social conversation going."
    in 31..50 -> "Communicate clearly in Android development work."
    else -> "Speak independently across everyday and work scenes."
}

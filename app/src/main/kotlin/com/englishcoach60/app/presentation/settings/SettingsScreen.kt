package com.englishcoach60.app.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.englishcoach60.designsystem.CoachCard
import com.englishcoach60.designsystem.PremiumTopAppBar
import com.englishcoach60.app.presentation.components.DifficultyCardPager
import com.englishcoach60.domain.model.TrainingMode
import com.englishcoach60.domain.training.DifficultyProfiles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    var baseUrl by remember(state.settings.baseUrl) { mutableStateOf(state.settings.baseUrl) }
    var model by remember(state.settings.model) { mutableStateOf(state.settings.model) }
    var resetStage by remember { mutableIntStateOf(0) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { PremiumTopAppBar("Settings", navigationIcon = { IconButton(onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } }) },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Text("Make each session feel right for you.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

            SettingsSection(Icons.Outlined.AutoAwesome, "AI coach", "Connection and conversation model") {
                if (!state.settings.hasApiKey) {
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Info, null); Spacer(Modifier.width(10.dp))
                            Text("Demo Mode · add DEEPSEEK_API_KEY to local.properties", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                OutlinedTextField(baseUrl, { baseUrl = it }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                OutlinedTextField(model, { model = it }, label = { Text("Model") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                Button(
                    onClick = { viewModel.update { it.copy(baseUrl = baseUrl, model = model) } },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) { Icon(Icons.Outlined.Save, null); Spacer(Modifier.width(8.dp)); Text("Save AI settings") }
                OutlinedButton(
                    onClick = viewModel::testConnection,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    enabled = !state.testing && state.settings.hasApiKey,
                ) {
                    if (state.testing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else { Icon(Icons.Outlined.Cable, null); Spacer(Modifier.width(8.dp)); Text("Test Connection") }
                }
            }

            SettingsSection(Icons.Outlined.RecordVoiceOver, "Voice", "Accent and playback pace") {
                ChoiceRow("American English", "en-US", state.settings.englishAccent == "en-US") { viewModel.update { it.copy(englishAccent = "en-US") } }
                ChoiceRow("British English", "en-GB", state.settings.englishAccent == "en-GB") { viewModel.update { it.copy(englishAccent = "en-GB") } }
                Text("Speech speed", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val rates = listOf(.85f to "0.85×", 1f to "1.0×", null to "Auto")
                    rates.forEachIndexed { index, (rate, label) ->
                        SegmentedButton(
                            selected = state.settings.ttsRateOverride == rate,
                            onClick = { viewModel.update { it.copy(ttsRateOverride = rate) } },
                            shape = SegmentedButtonDefaults.itemShape(index, rates.size),
                        ) { Text(label) }
                    }
                }
            }

            SettingsSection(Icons.Outlined.Speed, "Today's difficulty", "Swipe to tune the challenge threshold") {
                val profile = DifficultyProfiles.get(state.settings.difficulty)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Level ${profile.level} · ${profile.name}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    SuggestionChip(onClick = {}, enabled = false, label = { Text("Auto-adaptive") })
                }
                Text(
                    "Raise it when today's practice feels easy; lower it when you need more support. Your choice also becomes the coach's new adaptive baseline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DifficultyCardPager(
                    selectedLevel = state.settings.difficulty,
                    onDifficultyChange = { level -> viewModel.update { it.copy(difficulty = level) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SettingsSection(Icons.Outlined.Tune, "Training rhythm", "Choose your daily session length") {
                TrainingMode.entries.forEach { mode ->
                    ChoiceRow(
                        mode.name.lowercase().replaceFirstChar { it.uppercase() },
                        modeDescription(mode),
                        state.settings.trainingMode == mode,
                    ) { viewModel.update { it.copy(trainingMode = mode) } }
                }
            }

            SettingsSection(Icons.Outlined.Palette, "Appearance", "Comfortable in every light") {
                listOf("SYSTEM", "LIGHT", "DARK").forEach { mode ->
                    ChoiceRow(
                        mode.lowercase().replaceFirstChar { it.uppercase() },
                        when (mode) { "SYSTEM" -> "Follow your phone"; "LIGHT" -> "Warm paper"; else -> "Deep forest" },
                        state.settings.themeMode == mode,
                    ) { viewModel.update { it.copy(themeMode = mode) } }
                }
            }

            SettingsSection(Icons.Outlined.Storage, "Your data", "Stored privately on this device") {
                OutlinedButton(
                    onClick = { resetStage = 1 },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .45f)),
                ) { Icon(Icons.Outlined.DeleteSweep, null); Spacer(Modifier.width(8.dp)); Text("Reset Progress") }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
    state.connectionMessage?.let {
        AlertDialog(onDismissRequest = viewModel::clearMessage, icon = { Icon(Icons.Outlined.CloudDone, null) }, title = { Text("AI Connection") }, text = { Text(it) }, confirmButton = { TextButton(viewModel::clearMessage) { Text("OK") } })
    }
    if (state.resetComplete) AlertDialog(onDismissRequest = viewModel::clearMessage, title = { Text("Progress reset") }, text = { Text("The app will start again at Day 1.") }, confirmButton = { TextButton(viewModel::clearMessage) { Text("OK") } })
    if (resetStage > 0) AlertDialog(
        onDismissRequest = { resetStage = 0 },
        icon = { Icon(Icons.Outlined.WarningAmber, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(if (resetStage == 1) "Reset all progress?" else "Confirm permanent reset") },
        text = { Text(if (resetStage == 1) "This will delete all 60-day training progress." else "Lessons, conversations, expressions, reviews, and metrics will be deleted from this device.") },
        confirmButton = { TextButton({ if (resetStage == 1) resetStage = 2 else { viewModel.reset(); resetStage = 0 } }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text(if (resetStage == 1) "Continue" else "Delete everything") } },
        dismissButton = { TextButton({ resetStage = 0 }) { Text("Cancel") } },
    )
}

@Composable
private fun SettingsSection(icon: ImageVector, title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(icon, null, Modifier.padding(9.dp).size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column { Text(title, style = MaterialTheme.typography.titleLarge); Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        CoachCard(Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun ChoiceRow(label: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().selectable(selected, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .28f) else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected, onClick = null)
            Spacer(Modifier.width(8.dp))
            Column { Text(label, style = MaterialTheme.typography.labelLarge); Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

private fun modeDescription(mode: TrainingMode) = when (mode.name) {
    "QUICK" -> "A focused 25-minute session"
    "INTENSIVE" -> "A deeper 60-minute workout"
    else -> "A balanced 45-minute practice"
}

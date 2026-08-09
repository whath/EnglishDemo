package com.englishcoach60.app.presentation.training

import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.englishcoach60.app.presentation.components.DifficultyCardPager
import com.englishcoach60.domain.model.TrainingStep
import com.englishcoach60.domain.training.DifficultyProfiles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(viewModel: TrainingViewModel, onClose: () -> Unit, onCompleted: () -> Unit) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    var showClose by remember { mutableStateOf(false) }
    var showDifficulty by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startMic() else permissionDenied = true
    }
    val startMic = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) viewModel.startMic()
        else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
    BackHandler {
        if (state.step == TrainingStep.RECALL) showClose = true else viewModel.previousStep()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (state.step == TrainingStep.RECALL) Spacer(Modifier.size(48.dp))
                    else IconButton(onClick = viewModel::previousStep) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Previous step") }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DAY ${state.day}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(stepName(state.step), style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(onClick = { showClose = true }) { Icon(Icons.Outlined.Close, "Leave training") }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(6) { index ->
                        Surface(
                            modifier = Modifier.weight(1f).height(5.dp),
                            shape = CircleShape,
                            color = if (index < state.stepNumber) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            content = {},
                        )
                    }
                }
                Row(Modifier.align(Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    Text("Step ${state.stepNumber} of 6", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    AssistChip(
                        onClick = { showDifficulty = true },
                        label = {
                            Text(
                                "L${state.settings.difficulty} · ${compactDifficultyName(state.settings.difficulty)}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        leadingIcon = { Icon(Icons.Outlined.Tune, null, Modifier.size(16.dp)) },
                        modifier = Modifier.widthIn(max = 248.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                TrainingWordSearchBar(state, viewModel)
            }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).navigationBarsPadding()) {
            if (state.loading) CircularProgressIndicator(Modifier.align(Alignment.Center))
            else AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    (slideInHorizontally(tween(280)) { it / 5 } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(200)) { -it / 7 } + fadeOut(tween(160)))
                },
                label = "trainingStep",
            ) { step ->
                when (step) {
                    TrainingStep.RECALL -> RecallStep(state, viewModel::revealRecall, viewModel::rateRecall, viewModel::skipEmptyRecall)
                    TrainingStep.LISTENING -> ListeningStep(state, viewModel)
                    TrainingStep.REPEAT -> RepeatStep(state, viewModel, startMic)
                    TrainingStep.SPEAKING -> SpeakingStep(state, viewModel, startMic)
                    TrainingStep.RETELLING -> RetellingStep(state, viewModel, startMic)
                    TrainingStep.REVIEW -> ReviewStep(state) { viewModel.completeDay(onCompleted) }
                }
            }
        }
    }

    if (showClose) AlertDialog(onDismissRequest = { showClose = false }, title = { Text("Your progress is saved") },
        text = { Text("Leave training?") }, confirmButton = { TextButton(onClick = onClose) { Text("Leave") } },
        dismissButton = { TextButton(onClick = { showClose = false }) { Text("Keep Training") } })
    TrainingWordLookupDialog(state, viewModel)
    if (permissionDenied) AlertDialog(
        onDismissRequest = { permissionDenied = false },
        title = { Text("Microphone permission needed") },
        text = { Text("Allow microphone access in system settings to use speech recognition. Keyboard input remains available.") },
        confirmButton = {
            TextButton(
                onClick = {
                    permissionDenied = false
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                },
            ) { Text("Open settings") }
        },
        dismissButton = { TextButton(onClick = { permissionDenied = false }) { Text("Use keyboard") } },
    )
    state.error?.let { message ->
        val isSpeechError = message.contains("speech", ignoreCase = true) ||
            message.contains("microphone", ignoreCase = true) ||
            message.contains("recognition", ignoreCase = true)
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text(if (isSpeechError) "Speech recognition unavailable" else "Couldn't continue") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::clearError) {
                    Text(if (isSpeechError) "Use keyboard" else "OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (isSpeechError) {
                            viewModel.clearError()
                            startMic()
                        } else {
                            viewModel.retryLoad()
                        }
                    },
                ) { Text("Try again") }
            },
        )
    }

    if (showDifficulty) {
        var pendingDifficulty by remember(showDifficulty) { mutableIntStateOf(state.settings.difficulty) }
        ModalBottomSheet(
            onDismissRequest = { showDifficulty = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Tune today's difficulty", style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (state.step == TrainingStep.RECALL) {
                        "The complete lesson will be regenerated for the selected level before listening starts."
                    } else {
                        "The complete lesson will be regenerated for the selected level. Current lesson progress will restart from Listening."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DifficultyCardPager(
                    selectedLevel = pendingDifficulty,
                    onDifficultyChange = { pendingDifficulty = it },
                    modifier = Modifier.fillMaxWidth(),
                    compact = true,
                )
                Button(
                    onClick = { viewModel.setDifficulty(pendingDifficulty); showDifficulty = false },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                ) { Text("Apply level $pendingDifficulty") }
            }
        }
    }
}

private fun stepName(step: TrainingStep) = when (step) {
    TrainingStep.RECALL -> "Recall"; TrainingStep.LISTENING -> "Listening"; TrainingStep.REPEAT -> "Listen & Repeat"
    TrainingStep.SPEAKING -> "Speaking"; TrainingStep.RETELLING -> "Retelling"; TrainingStep.REVIEW -> "Daily Review"
}

private fun compactDifficultyName(level: Int) = when (level.coerceIn(1, 4)) {
    1 -> "University Foundation"
    2 -> "University Plus"
    3 -> "Advanced"
    else -> "Professional Challenge"
}

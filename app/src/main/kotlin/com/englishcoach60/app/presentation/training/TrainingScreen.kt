package com.englishcoach60.app.presentation.training

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
                    else IconButton(onClick = viewModel::previousStep) { Icon(Icons.Outlined.ArrowBack, "Previous step") }
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
                        label = { Text("L${state.settings.difficulty} · ${DifficultyProfiles.get(state.settings.difficulty).name}") },
                        leadingIcon = { Icon(Icons.Outlined.Tune, null, Modifier.size(16.dp)) },
                    )
                }
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
    if (permissionDenied) AlertDialog(onDismissRequest = { permissionDenied = false }, title = { Text("Microphone permission needed") },
        text = { Text("Microphone permission is needed for speaking practice. You can still use the keyboard.") },
        confirmButton = { TextButton(onClick = { permissionDenied = false }) { Text("Use keyboard") } })
    state.error?.let { message -> AlertDialog(onDismissRequest = viewModel::clearError, title = { Text("Couldn't continue") }, text = { Text(message) },
        confirmButton = { TextButton(onClick = viewModel::clearError) { Text("OK") } },
        dismissButton = { TextButton(onClick = viewModel::retryLoad) { Text("Try again") } }) }

    if (showDifficulty) {
        var pendingDifficulty by remember(showDifficulty) { mutableIntStateOf(state.settings.difficulty) }
        ModalBottomSheet(
            onDismissRequest = { showDifficulty = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Tune today's difficulty", style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (state.step == TrainingStep.RECALL) "Your lesson will be refreshed before listening starts."
                    else "The new level applies to audio pace and the next AI reply. Completed material stays unchanged.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DifficultyCardPager(
                    selectedLevel = pendingDifficulty,
                    onDifficultyChange = { pendingDifficulty = it },
                    modifier = Modifier.fillMaxWidth(),
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

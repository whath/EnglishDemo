package com.englishcoach60.app.presentation.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.englishcoach60.designsystem.SectionLabel
import com.englishcoach60.domain.language.containsHanCharacters

@Composable
internal fun TrainingWordSearchBar(state: TrainingUiState, viewModel: TrainingViewModel) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = state.wordSearchQuery,
        onValueChange = viewModel::setWordSearchQuery,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        placeholder = { Text("中文 → English · English → 中文") },
        leadingIcon = { Icon(Icons.Outlined.Search, null) },
        trailingIcon = {
            FilledIconButton(
                onClick = {
                    keyboard?.hide()
                    viewModel.searchWord()
                },
                enabled = !state.wordLookupLoading && state.wordSearchQuery.isNotBlank(),
                modifier = Modifier.size(36.dp),
            ) {
                if (state.wordLookupLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.Search, "Search word")
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                keyboard?.hide()
                viewModel.searchWord()
            },
        ),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = MaterialTheme.shapes.large,
    )
}

@Composable
internal fun TrainingWordLookupDialog(state: TrainingUiState, viewModel: TrainingViewModel) {
    if (!state.wordLookupDialogVisible) return
    val result = state.wordLookupResult
    val isChineseQuery = state.wordSearchQuery.containsHanCharacters()

    Dialog(
        onDismissRequest = viewModel::dismissWordLookup,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            Modifier.fillMaxWidth().padding(20.dp).heightIn(max = 680.dp),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionLabel(
                            if (result == null) "Word Studio"
                            else if (isChineseQuery) "中文 → English"
                            else "English → 中文",
                        )
                        Text(
                            result?.let { if (isChineseQuery) it.word else it.meaningZh }
                                ?: state.wordSearchQuery.ifBlank { "Dictionary" },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    state.wordLookupResult?.let {
                        FilledTonalIconButton(onClick = viewModel::speakLookupWord) {
                            Icon(Icons.AutoMirrored.Outlined.VolumeUp, "Play pronunciation")
                        }
                    }
                }

                when {
                    state.wordLookupLoading -> {
                        Box(
                            Modifier.fillMaxWidth().height(220.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    "Finding a natural meaning and example…",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    result != null -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (!isChineseQuery) {
                                Text(
                                    result.word,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            if (result.phonetic.isNotBlank()) {
                                Text(
                                    result.phonetic,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            if (result.partOfSpeech.isNotBlank()) {
                                Text(result.partOfSpeech, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider()
                        if (isChineseQuery) {
                            SectionLabel("中文释义")
                            Text(result.meaningZh, style = MaterialTheme.typography.titleLarge)
                        }
                        if (result.definitionEnglish.isNotBlank()) {
                            Text(result.definitionEnglish, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .55f),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SectionLabel("Example")
                                    Spacer(Modifier.weight(1f))
                                    IconButton(onClick = viewModel::speakLookupExample) {
                                        Icon(Icons.AutoMirrored.Outlined.VolumeUp, "Play example")
                                    }
                                }
                                Text(result.example, style = MaterialTheme.typography.titleMedium)
                                if (result.exampleZh.isNotBlank()) {
                                    Text(result.exampleZh, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (result.relatedExpressions.isNotEmpty()) {
                            SectionLabel("Useful combinations")
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                result.relatedExpressions.forEach { related ->
                                    AssistChip(
                                        onClick = { viewModel.searchRelatedWord(related) },
                                        label = { Text(related) },
                                    )
                                }
                            }
                        }
                    }
                }

                state.wordLookupMessage?.let { message ->
                    Text(
                        message,
                        color = if (message.startsWith("Saved")) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                ) {
                    TextButton(onClick = viewModel::dismissWordLookup) { Text("Close") }
                    if (state.wordLookupResult != null) {
                        Button(onClick = viewModel::saveLookupResult) {
                            Icon(Icons.Outlined.BookmarkAdd, null)
                            Spacer(Modifier.size(8.dp))
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

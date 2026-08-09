package com.englishcoach60.app.presentation.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.englishcoach60.designsystem.CoachCard
import com.englishcoach60.designsystem.SectionLabel
import com.englishcoach60.domain.language.containsHanCharacters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: SearchViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Word Studio") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).navigationBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("Find the words you want to use", style = MaterialTheme.typography.headlineMedium)
            Text("中文查英文，English 查中文，并提供发音与实用例句。", color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                placeholder = { Text("试试“自信”或“confident”") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = {
                    FilledIconButton(
                        onClick = { keyboard?.hide(); viewModel.search() },
                        enabled = !state.loading && state.query.isNotBlank(),
                    ) { Icon(Icons.Outlined.Search, "Search") }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); viewModel.search() }),
                singleLine = true,
                shape = MaterialTheme.shapes.large,
            )

            AnimatedVisibility(state.suggestions.isNotEmpty() && state.result == null && !state.loading) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(if (state.query.isBlank()) "From your library" else "Matches in your library")
                    state.suggestions.forEach { item ->
                        SuggestionChip(
                            onClick = { viewModel.useSuggestion(item) },
                            label = { Text(item.expression) },
                        )
                    }
                }
            }

            AnimatedContent(targetState = state.loading to state.result, label = "dictionaryResult") { (loading, result) ->
                when {
                    loading -> Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            CircularProgressIndicator()
                            Text("Looking for a useful, natural example…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    result != null -> {
                        val isChineseQuery = state.query.containsHanCharacters()
                        CoachCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.Top) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    SectionLabel(if (isChineseQuery) "中文 → English" else "English → 中文")
                                    Text(
                                        if (isChineseQuery) result.word else result.meaningZh,
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        if (!isChineseQuery) {
                                            Text(result.word, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                                        }
                                        if (result.phonetic.isNotBlank()) Text(result.phonetic, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                                        if (result.partOfSpeech.isNotBlank()) Text(result.partOfSpeech, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                FilledTonalIconButton(onClick = viewModel::speakWord) { Icon(Icons.AutoMirrored.Outlined.VolumeUp, "Play pronunciation") }
                            }
                            HorizontalDivider()
                            if (isChineseQuery) {
                                SectionLabel("中文释义")
                                Text(result.meaningZh, style = MaterialTheme.typography.titleLarge)
                            }
                            if (result.definitionEnglish.isNotBlank()) Text(result.definitionEnglish, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .55f), shape = MaterialTheme.shapes.medium) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SectionLabel("Example")
                                        Spacer(Modifier.weight(1f))
                                        IconButton(onClick = viewModel::speakExample) { Icon(Icons.AutoMirrored.Outlined.VolumeUp, "Play example") }
                                    }
                                    Text(result.example, style = MaterialTheme.typography.titleMedium)
                                    if (result.exampleZh.isNotBlank()) Text(result.exampleZh, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (result.relatedExpressions.isNotEmpty()) {
                                SectionLabel("Useful combinations")
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    result.relatedExpressions.forEach { related -> AssistChip(onClick = { viewModel.setQuery(related) }, label = { Text(related) }) }
                                }
                            }
                            Button(onClick = viewModel::saveResult, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
                                Icon(Icons.Outlined.BookmarkAdd, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Save to My Expressions")
                            }
                        }
                    }
                    else -> CoachCard(Modifier.fillMaxWidth()) {
                        SectionLabel("Build active vocabulary")
                        Text("Search a word you recently heard or wanted to say.", style = MaterialTheme.typography.titleLarge)
                        Text("Tap the speaker to hear it, then reuse the example aloud.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            state.message?.let { message ->
                Snackbar(
                    action = { TextButton(onClick = viewModel::clearMessage) { Text("OK") } },
                ) { Text(message) }
            }
        }
    }
}

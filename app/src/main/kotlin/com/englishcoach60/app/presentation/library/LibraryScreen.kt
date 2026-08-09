package com.englishcoach60.app.presentation.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.englishcoach60.designsystem.PremiumTopAppBar
import com.englishcoach60.designsystem.SectionLabel
import com.englishcoach60.domain.model.Expression
import com.englishcoach60.domain.model.ReviewRating
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(viewModel: LibraryViewModel, onBack: () -> Unit) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    var expanded by remember { mutableStateOf<Expression?>(null) }
    val now = System.currentTimeMillis()
    val filtered = when (state.filter) {
        LibraryFilter.DUE -> state.expressions.filter { it.nextReviewAt <= now }
        LibraryFilter.PINNED -> state.expressions.filter { it.pinned }
        LibraryFilter.ALL -> state.expressions
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PremiumTopAppBar(
                title = "My Expressions",
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).navigationBarsPadding()) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text("A personal phrasebook that remembers when you should practise.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(18.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    LibraryFilter.entries.forEachIndexed { i, filter ->
                        SegmentedButton(
                            selected = state.filter == filter,
                            onClick = { viewModel.filter(filter) },
                            shape = SegmentedButtonDefaults.itemShape(i, LibraryFilter.entries.size),
                        ) { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    }
                }
            }
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer) {
                            Icon(Icons.Outlined.BookmarkBorder, null, Modifier.padding(20.dp).size(32.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Text(if (state.filter == LibraryFilter.DUE) "You're all caught up" else "Your collection is quiet", style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (state.filter == LibraryFilter.DUE) "Keep training. Expressions will return at the right time." else "Save useful expressions during training and find them here.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionLabel("${filtered.size} saved")
                            Spacer(Modifier.weight(1f))
                            Text("Tap to practise", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(filtered, key = { it.expression }) { item -> ExpressionCard(item, now) { expanded = item } }
                }
            }
        }
    }
    expanded?.let { item ->
        ModalBottomSheet(onDismissRequest = { expanded = null }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionLabel("Expression practice")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.expression, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                    FilledTonalIconButton({ viewModel.speak(item) }) { Icon(Icons.AutoMirrored.Outlined.VolumeUp, "Play expression") }
                }
                Text(item.meaningZh, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(item.example, Modifier.fillMaxWidth().padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                }
                Text("How well did you remember it?", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ viewModel.review(item, ReviewRating.AGAIN) }, Modifier.weight(1f)) { Text("Again") }
                    OutlinedButton({ viewModel.review(item, ReviewRating.HARD) }, Modifier.weight(1f)) { Text("Hard") }
                    Button({ viewModel.review(item, ReviewRating.GOOD) }, Modifier.weight(1f)) { Text("Good") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton({ viewModel.pin(item) }) { Icon(Icons.Outlined.PushPin, null); Spacer(Modifier.width(6.dp)); Text(if (item.pinned) "Unpin" else "Pin") }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = { viewModel.delete(item); expanded = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Icon(Icons.Outlined.Delete, null); Spacer(Modifier.width(6.dp)); Text("Delete") }
                }
            }
        }
    }
}

@Composable
private fun ExpressionCard(item: Expression, now: Long, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.expression, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f, fill = false))
                    if (item.pinned) Icon(Icons.Outlined.PushPin, "Pinned", Modifier.padding(start = 8.dp).size(17.dp), tint = MaterialTheme.colorScheme.secondary)
                }
                Text(item.meaningZh, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { index ->
                        Box(
                            Modifier.padding(end = 4.dp).size(width = 15.dp, height = 4.dp),
                        ) {
                            HorizontalDivider(thickness = 4.dp, color = if (index < item.mastery) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (item.nextReviewAt <= now) "Due now" else DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(item.nextReviewAt)),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (item.nextReviewAt <= now) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

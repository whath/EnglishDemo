package com.englishcoach60.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.englishcoach60.domain.training.DifficultyProfiles
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun DifficultyCardPager(
    selectedLevel: Int,
    onDifficultyChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val profiles = remember { DifficultyProfiles.all() }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = selectedLevel.coerceIn(1, 4) - 1,
        pageCount = { profiles.size },
    )
    val currentSelection by rememberUpdatedState(selectedLevel.coerceIn(1, 4))
    val currentOnChange by rememberUpdatedState(onDifficultyChange)

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val level = page + 1
                if (level != currentSelection) currentOnChange(level)
            }
    }
    LaunchedEffect(selectedLevel) {
        val page = selectedLevel.coerceIn(1, 4) - 1
        if (!pagerState.isScrollInProgress && pagerState.settledPage != page) pagerState.animateScrollToPage(page)
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = if (compact) 8.dp else 14.dp),
            pageSpacing = 10.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val profile = profiles[page]
            val selected = pagerState.settledPage == page
            Surface(
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = "Difficulty level ${profile.level}, ${profile.name}: ${profile.summary}"
                },
                shape = MaterialTheme.shapes.large,
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .35f) else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(if (compact) 15.dp else 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                        Text(
                            profile.level.toString(),
                            Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(profile.name, style = MaterialTheme.typography.titleMedium)
                        Text(profile.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { if (pagerState.currentPage > 0) scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                enabled = pagerState.currentPage > 0,
            ) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Lower difficulty") }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                repeat(profiles.size) { index ->
                    Surface(
                        modifier = Modifier.padding(horizontal = 3.dp).size(if (index == pagerState.settledPage) 18.dp else 7.dp, 7.dp),
                        shape = CircleShape,
                        color = if (index == pagerState.settledPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        content = {},
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Outlined.Swipe, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text("Swipe", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(
                onClick = { if (pagerState.currentPage < profiles.lastIndex) scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                enabled = pagerState.currentPage < profiles.lastIndex,
            ) { Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Raise difficulty") }
        }
    }
}

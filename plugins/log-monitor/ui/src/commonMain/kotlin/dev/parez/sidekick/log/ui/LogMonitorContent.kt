package dev.parez.sidekick.log.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import dev.parez.sidekick.log.LogEntry
import dev.parez.sidekick.log.LogLevel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun LogMonitorContent(
    lazyItems: LazyPagingItems<LogEntry>,
    selected: LogEntry?,
    query: String,
    levelFilter: Set<LogLevel>,
    filteredCount: Long,
    onSelect: (String?) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleLevel: (LogLevel) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val scope = rememberCoroutineScope()

    LaunchedEffect(selected?.id) {
        if (selected != null) {
            navigator.navigateTo(
                pane = ListDetailPaneScaffoldRole.Detail,
                contentKey = selected.id,
            )
        }
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                LogEntryListPane(
                    lazyItems = lazyItems,
                    selected = selected,
                    query = query,
                    levelFilter = levelFilter,
                    filteredCount = filteredCount,
                    onSelect = { entry -> onSelect(entry.id) },
                    onQueryChange = onQueryChange,
                    onToggleLevel = onToggleLevel,
                    onClear = onClear,
                    showChevron = true,
                    onBack = onBack,
                )
            }
        },
        detailPane = {
            AnimatedPane {
                if (selected != null) {
                    LogEntryDetailPane(
                        entry = selected,
                        showBackButton = true,
                        onBack = {
                            onSelect(null)
                            scope.launch { navigator.navigateBack() }
                        },
                    )
                } else {
                    DetailEmptyState()
                }
            }
        },
    )
}

@Composable
internal fun DetailEmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                Icons.Default.TouchApp,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
                text = "Select a log entry",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = "Tap a log entry on the left to inspect it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

package dev.parez.sidekick.network.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneExpansionAnchor
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import dev.parez.sidekick.network.NetworkCall
import kotlinx.coroutines.launch

/**
 * Root composable for the Network Monitor plugin. Uses Material 3 Adaptive ListDetailPaneScaffold:
 * - On compact screens: single-pane push navigation.
 * - On wider screens: side-by-side list + detail panes.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun NetworkMonitorContent(
    lazyItems: LazyPagingItems<NetworkCall>,
    selected: NetworkCall?,
    query: String,
    methodFilter: Set<String>,
    filteredCount: Long,
    onSelect: (String?) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleMethod: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val scope = rememberCoroutineScope()

    LaunchedEffect(selected?.id) {
        if (selected != null) {
            navigator.navigateTo(pane = ListDetailPaneScaffoldRole.Detail, contentKey = selected.id)
        }
    }

    // The extra pane is "Expanded" when the window is wide enough for three
    // panes — on those layouts Response moves out of the detail tabs and into
    // its own pane (see `NetworkCallResponsePane`).
    val extraVisible =
        navigator.scaffoldValue[ListDetailPaneScaffoldRole.Extra] == PaneAdaptedValue.Expanded

    // Proportion-based anchors keep the divider in step with window resizes;
    // dp-absolute defaults would drift the divider visually as the host shrinks.
    val paneExpansionState =
        rememberPaneExpansionState(
            anchors =
                listOf(
                    PaneExpansionAnchor.Proportion(0.3f),
                    PaneExpansionAnchor.Proportion(0.5f),
                    PaneExpansionAnchor.Proportion(0.7f),
                ),
            initialAnchoredIndex = 1,
        )

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        paneExpansionState = paneExpansionState,
        paneExpansionDragHandle = { state ->
            val interactionSource = remember { MutableInteractionSource() }
            VerticalDragHandle(
                modifier =
                    Modifier.paneExpansionDraggable(
                        state = state,
                        minTouchTargetSize = 48.dp,
                        interactionSource = interactionSource,
                        semanticsProperties = null,
                    ),
                interactionSource = interactionSource,
            )
        },
        listPane = {
            AnimatedPane {
                NetworkCallListPane(
                    lazyItems = lazyItems,
                    selected = selected,
                    query = query,
                    methodFilter = methodFilter,
                    filteredCount = filteredCount,
                    onSelect = { call -> onSelect(call.id) },
                    onQueryChange = onQueryChange,
                    onToggleMethod = onToggleMethod,
                    onClear = onClear,
                    showChevron = true,
                    onBack = onBack,
                )
            }
        },
        detailPane = {
            AnimatedPane {
                if (selected != null) {
                    NetworkCallDetailPane(
                        call = selected,
                        showBackButton = true,
                        onBack = {
                            onSelect(null)
                            scope.launch { navigator.navigateBack() }
                        },
                        hideResponseTab = extraVisible,
                    )
                } else {
                    DetailEmptyState()
                }
            }
        },
        extraPane = {
            AnimatedPane {
                if (selected != null) {
                    NetworkCallResponsePane(call = selected)
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
                text = "Select a request",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = "Tap a network call on the left to inspect it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

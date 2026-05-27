package dev.parez.sidekick.network.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.adaptive.layout.PaneExpansionAnchor
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import dev.parez.sidekick.network.NetworkCall
import dev.parez.sidekick.network.ui.persistence.NetworkMonitorPaneSizes
import dev.parez.sidekick.network.ui.persistence.createPaneSizeStore
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Root composable for the Network Monitor plugin. Uses Material 3 Adaptive ListDetailPaneScaffold
 * for the list↔detail split and a custom in-pane Row for Request↔Response on wide windows:
 * - **Compact / medium**: single Detail pane with Request / Response tabs
 *   ([NetworkCallDetailPane]).
 * - **Expanded**: Detail pane renders [NetworkCallDetailSplit] — Request and Response side-by-side
 *   in a Row with a draggable splitter between them.
 *
 * Both the list↔detail anchor and the request↔response splitter position are persisted per device
 * via [createPaneSizeStore].
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

    // Three discrete anchor positions for the list↔detail boundary. Centered on
    // ~33 % so the default layout — combined with the inner 50/50 split between
    // Request and Response — reads as roughly 33/33/33 across all three panes.
    val anchors = remember {
        listOf(
            PaneExpansionAnchor.Proportion(0.2f),
            PaneExpansionAnchor.Proportion(0.33f),
            PaneExpansionAnchor.Proportion(0.5f),
        )
    }
    val paneExpansionState = rememberPaneExpansionState(anchors = anchors, initialAnchoredIndex = 1)

    // Splitter proportion between Request (left) and Response (right) inside the
    // expanded detail layout. 0.5f = even split; clamped 0.2..0.8 by the
    // splitter handle so neither column can collapse.
    var splitProportion by remember { mutableFloatStateOf(0.5f) }

    // Persist + restore both knobs via the platform-native key/value store.
    val store = remember { createPaneSizeStore() }
    LaunchedEffect(Unit) {
        store.read()?.let { saved ->
            val clampedIndex = saved.listAnchorIndex.coerceIn(0, anchors.size - 1)
            paneExpansionState.animateTo(anchors[clampedIndex])
            splitProportion = saved.requestResponseProportion.coerceIn(0.2f, 0.8f)
        }
    }
    LaunchedEffect(paneExpansionState, anchors) {
        snapshotFlow {
                val current = paneExpansionState.currentAnchor
                val index =
                    if (current is PaneExpansionAnchor.Proportion) {
                        anchors.indexOf(current).takeIf { it >= 0 } ?: 1
                    } else 1
                NetworkMonitorPaneSizes(
                    listAnchorIndex = index,
                    requestResponseProportion = splitProportion,
                )
            }
            .drop(1)
            .distinctUntilChanged()
            .collect { store.write(it) }
    }

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
                    // Use the side-by-side split only when the detail pane is wide
                    // enough that both columns end up bigger than the splitter
                    // handle + each pane's minimum content width. 600 dp matches
                    // M3's "expanded" width-class threshold.
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        if (maxWidth >= 600.dp) {
                            NetworkCallDetailSplit(
                                call = selected,
                                proportion = splitProportion,
                                onProportionChange = { splitProportion = it },
                            )
                        } else {
                            NetworkCallDetailPane(
                                call = selected,
                                showBackButton = true,
                                onBack = {
                                    onSelect(null)
                                    scope.launch { navigator.navigateBack() }
                                },
                            )
                        }
                    }
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

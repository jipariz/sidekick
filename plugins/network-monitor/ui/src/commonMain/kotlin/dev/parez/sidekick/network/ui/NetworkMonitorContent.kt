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
 * Root composable for the Network Monitor plugin. Uses Material 3 Adaptive ListDetailPaneScaffold
 * for the list↔detail split and a custom in-pane Row for Request↔Response on wide windows:
 * - **Compact / medium**: single Detail pane with Request / Response tabs
 *   ([NetworkCallDetailPane]).
 * - **Expanded**: Detail pane renders [NetworkCallDetailSplit] — Request and Response side-by-side
 *   in a Row with a draggable splitter between them.
 *
 * The list↔detail anchor and the request↔response splitter position both reset to defaults each
 * time the plugin is opened — no cross-session persistence.
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

    // Dp-absolute anchor positions for the list↔detail boundary. Offset
    // anchors keep the divider at a fixed dp from the start edge across window
    // resizes — drag to 320 dp and grow the window, list stays at 320 dp while
    // the detail side absorbs the extra space.
    val anchors = remember {
        listOf(
            PaneExpansionAnchor.Offset.fromStart(240.dp),
            PaneExpansionAnchor.Offset.fromStart(320.dp),
            PaneExpansionAnchor.Offset.fromStart(400.dp),
        )
    }
    val paneExpansionState = rememberPaneExpansionState(anchors = anchors, initialAnchoredIndex = 1)

    // M3 Adaptive can decide to hide the list pane in narrower windows (single-
    // pane navigation). When that happens we must keep a back button on the
    // detail pane so the user can return — and we must NOT show the side-by-
    // side split layout (which has no back affordance) regardless of the
    // detail pane's own measured width.
    val listVisible =
        navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded

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
                    // Side-by-side split only when the list is also visible AND
                    // the detail column itself is wide enough. If the list is
                    // hidden (single-pane mode), force the tabbed layout — it
                    // has the back button the user needs to return to the list.
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        if (listVisible && maxWidth >= 600.dp) {
                            NetworkCallDetailSplit(call = selected)
                        } else {
                            NetworkCallDetailPane(
                                call = selected,
                                showBackButton = !listVisible,
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

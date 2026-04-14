package dev.parez.sidekick.network.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import dev.parez.sidekick.network.NetworkCall
import kotlinx.serialization.Serializable

// ── Navigation keys ──────────────────────────────────────────────────────────

@Serializable
private data object NetworkListKey : NavKey

@Serializable
private data class NetworkDetailKey(val callId: String) : NavKey

// ── Root composable ──────────────────────────────────────────────────────────

/**
 * Root composable for the Network Monitor plugin.
 * Uses Navigation 3 with adaptive list-detail layout:
 * - On compact screens: single-pane push navigation.
 * - On wider screens: side-by-side list + detail panes.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun NetworkMonitorContent(
    calls: List<NetworkCall>,
    selected: NetworkCall?,
    onSelect: (NetworkCall) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    val backStack = buildList<NavKey> {
        add(NetworkListKey)
        if (selected != null) add(NetworkDetailKey(selected.id))
    }.toMutableStateList()

    val sceneStrategy = rememberListDetailSceneStrategy<NavKey>()

    NavDisplay(
        backStack = backStack,
        sceneStrategies = listOf(sceneStrategy),
        onBack = {
            onBack()
            true
        },
        entryProvider = entryProvider {
            entry<NetworkListKey>(
                metadata = ListDetailSceneStrategy.listPane(),
            ) {
                NetworkCallListPane(
                    calls = calls,
                    selected = selected,
                    onSelect = onSelect,
                    onClear = onClear,
                    showChevron = true,
                )
            }

            entry<NetworkDetailKey>(
                metadata = ListDetailSceneStrategy.detailPane(),
            ) { key ->
                val call = calls.firstOrNull { it.id == key.callId }
                if (call != null) {
                    NetworkCallDetailPane(
                        call = call,
                        showBackButton = true,
                        onBack = onBack,
                    )
                } else {
                    DetailEmptyState()
                }
            }
        },
    )
}

// ── Detail empty state ────────────────────────────────────────────────────────

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

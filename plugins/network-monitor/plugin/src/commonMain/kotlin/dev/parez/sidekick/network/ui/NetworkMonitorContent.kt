package dev.parez.sidekick.network.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.parez.sidekick.network.NetworkCall

// ── Adaptive breakpoints ──────────────────────────────────────────────────────

/** Compact → single-pane: list + push to detail. */
private val CompactBreakpoint = 600.dp

/** Expanded → fixed-width list pane at 360 dp. */
private val ExpandedBreakpoint = 840.dp

/** Fixed width of the list pane in expanded (desktop/web) layout. */
private val ExpandedListWidth = 360.dp

/**
 * Root composable for the Network Monitor plugin.
 * Picks a layout based on the available width:
 *
 * - **< 600 dp (compact/mobile)** — Single pane. The list navigates to the detail screen.
 * - **600–840 dp (medium/tablet)** — Two panes side-by-side at 40 / 60 split.
 * - **≥ 840 dp (expanded/desktop/web)** — Two panes: list fixed at 360 dp, detail fills rest.
 */
@Composable
internal fun NetworkMonitorContent(
    calls: List<NetworkCall>,
    selected: NetworkCall?,
    onSelect: (NetworkCall) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        when {
            maxWidth >= ExpandedBreakpoint -> ExpandedLayout(
                calls = calls,
                selected = selected,
                onSelect = onSelect,
                onClear = onClear,
                onDismiss = onBack,
            )
            maxWidth >= CompactBreakpoint -> MediumLayout(
                calls = calls,
                selected = selected,
                onSelect = onSelect,
                onClear = onClear,
                onDismiss = onBack,
            )
            else -> CompactLayout(
                calls = calls,
                selected = selected,
                onSelect = onSelect,
                onClear = onClear,
                onBack = onBack,
            )
        }
    }
}

// ── Compact layout (<600 dp) ──────────────────────────────────────────────────

@Composable
private fun CompactLayout(
    calls: List<NetworkCall>,
    selected: NetworkCall?,
    onSelect: (NetworkCall) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    if (selected == null) {
        NetworkCallListPane(
            calls = calls,
            onSelect = onSelect,
            onClear = onClear,
            showChevron = true,
        )
    } else {
        NetworkCallDetailPane(
            call = selected,
            showBackButton = true,
            onBack = onBack,
        )
    }
}

// ── Medium layout (600–840 dp) ────────────────────────────────────────────────

@Composable
private fun MediumLayout(
    calls: List<NetworkCall>,
    selected: NetworkCall?,
    onSelect: (NetworkCall) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(Modifier.fillMaxSize()) {
        // List pane — 40 %
        Surface(
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            NetworkCallListPane(
                calls = calls,
                selected = selected,
                onSelect = onSelect,
                onClear = onClear,
                showChevron = false,
            )
        }

        VerticalDivider()

        // Detail pane — 60 %
        Box(
            modifier = Modifier
                .weight(3f)
                .fillMaxHeight(),
        ) {
            if (selected != null) {
                NetworkCallDetailPane(
                    call = selected,
                    showBackButton = false,
                    onBack = onDismiss,
                )
            } else {
                DetailEmptyState()
            }
        }
    }
}

// ── Expanded layout (≥840 dp) ─────────────────────────────────────────────────

@Composable
private fun ExpandedLayout(
    calls: List<NetworkCall>,
    selected: NetworkCall?,
    onSelect: (NetworkCall) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(Modifier.fillMaxSize()) {
        // List pane — fixed 360 dp
        Surface(
            modifier = Modifier
                .width(ExpandedListWidth)
                .fillMaxHeight(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            NetworkCallListPane(
                calls = calls,
                selected = selected,
                onSelect = onSelect,
                onClear = onClear,
                showChevron = false,
            )
        }

        VerticalDivider()

        // Detail pane — fills remaining width
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            if (selected != null) {
                NetworkCallDetailPane(
                    call = selected,
                    showBackButton = false,
                    onBack = onDismiss,
                )
            } else {
                DetailEmptyState()
            }
        }
    }
}

// ── Detail empty state ────────────────────────────────────────────────────────

@Composable
private fun DetailEmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            androidx.compose.material3.Icon(
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
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

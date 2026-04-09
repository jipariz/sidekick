package dev.parez.sidekick.network.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.parez.sidekick.network.CallStatus
import dev.parez.sidekick.network.NetworkCall
import dev.parez.sidekick.plugin.LocalSidekickColors

/**
 * List pane for the Network Monitor. Used in both single-pane (compact) and
 * two-pane (medium/expanded) layouts.
 *
 * @param selected    The currently selected call; used to highlight the active row in two-pane mode.
 * @param showChevron Whether to show a trailing chevron (only meaningful in compact/single-pane).
 */
@Composable
internal fun NetworkCallListPane(
    calls: List<NetworkCall>,
    selected: NetworkCall? = null,
    onSelect: (NetworkCall) -> Unit,
    onClear: () -> Unit,
    showChevron: Boolean = true,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(calls, query) {
        if (query.isBlank()) calls
        else calls.filter {
            it.url.contains(query, ignoreCase = true) ||
                it.method.contains(query, ignoreCase = true)
        }
    }
    val activeCount = remember(calls) { calls.count { it.status == CallStatus.PENDING } }

    Column(Modifier.fillMaxSize()) {
        // ── Search bar ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Search URL or method…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                shape = MaterialTheme.shapes.small,
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onClear) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Clear all",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ── Stats row ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${calls.size} request${if (calls.size != 1) "s" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (activeCount > 0) {
                Icon(
                    Icons.Default.Wifi,
                    contentDescription = null,
                    tint = LocalSidekickColors.current.statusPending,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = "$activeCount active",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalSidekickColors.current.statusPending,
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // ── Content ───────────────────────────────────────────────────────────
        if (filtered.isEmpty()) {
            NetworkCallEmptyState(isFiltered = query.isNotBlank())
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                items(filtered, key = { it.id }) { call ->
                    NetworkCallRow(
                        call = call,
                        isSelected = selected?.id == call.id,
                        showChevron = showChevron,
                        onClick = { onSelect(call) },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 56.dp),
                    )
                }
            }
        }
    }
}

// ── List row ──────────────────────────────────────────────────────────────────

@Composable
private fun NetworkCallRow(
    call: NetworkCall,
    isSelected: Boolean,
    showChevron: Boolean,
    onClick: () -> Unit,
) {
    val selectionColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) selectionColor else Color.Transparent)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Method badge — left-aligned, padded to top
            MethodBadge(call.method, modifier = Modifier.padding(top = 2.dp))

            // URL + meta — fills available width
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                val host = urlHost(call.url)
                val path = urlPath(call.url)
                if (host.isNotEmpty()) {
                    Text(
                        text = host,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (path.isNotEmpty()) {
                        Text(
                            text = path,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    Text(
                        text = call.url,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Duration + response body size
                val meta = buildString {
                    call.durationMs?.let { append("${it}ms") }
                    call.responseBody?.let {
                        if (isNotEmpty()) append(" · ")
                        append(it.bodySizeLabel())
                    }
                }
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            // Status + optional chevron — right column
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                StatusChip(call)
                if (showChevron) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

// ── Method badge ──────────────────────────────────────────────────────────────

@Composable
internal fun MethodBadge(method: String, modifier: Modifier = Modifier) {
    val colors = LocalSidekickColors.current
    val bg = when (method.uppercase()) {
        "GET"    -> colors.httpGet
        "POST"   -> colors.httpPost
        "PUT"    -> colors.httpPut
        "DELETE" -> colors.httpDelete
        "PATCH"  -> colors.httpPatch
        else     -> colors.httpOther
    }
    Surface(
        color = bg,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier,
    ) {
        Text(
            text = method.uppercase().take(6),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onHttpBadge,
            fontFamily = FontFamily.Monospace,
        )
    }
}

// ── Status chip ───────────────────────────────────────────────────────────────

@Composable
internal fun StatusChip(call: NetworkCall) {
    val colors = LocalSidekickColors.current
    val (text, bg) = when (call.status) {
        CallStatus.PENDING  -> "●  PENDING" to colors.statusPending
        CallStatus.ERROR    -> "ERR"         to colors.statusNetworkError
        CallStatus.COMPLETE -> {
            val code = call.responseCode ?: 0
            val label = "$code ${statusText(code)}".trim()
            val c = when {
                code < 300 -> colors.statusSuccess
                code < 400 -> colors.statusRedirect
                code < 500 -> colors.statusClientError
                else       -> colors.statusServerError
            }
            label to c
        }
    }
    Surface(color = bg, shape = MaterialTheme.shapes.extraSmall) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onStatusChip,
            fontFamily = FontFamily.Monospace,
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun NetworkCallEmptyState(isFiltered: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Default.Wifi,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
                text = if (isFiltered) "No matching requests" else "No requests recorded",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (isFiltered) "Try a different search term" else "Make a network call to see it here",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

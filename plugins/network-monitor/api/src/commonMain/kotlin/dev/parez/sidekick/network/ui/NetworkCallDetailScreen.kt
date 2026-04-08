package dev.parez.sidekick.network.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.parez.sidekick.network.CallStatus
import dev.parez.sidekick.network.NetworkCall
import dev.parez.sidekick.plugin.LocalSidekickColors

/**
 * Detail pane for the Network Monitor. Works in both single-pane (compact) and
 * two-pane (medium/expanded) layouts.
 *
 * @param showBackButton When true (compact mode) shows a back arrow in the TopAppBar.
 *                       When false (two-pane) shows nothing (caller manages the panel).
 * @param onBack         Called when the back arrow is tapped (compact) or when the pane
 *                       should be dismissed (two-pane close button, if added).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NetworkCallDetailPane(
    call: NetworkCall,
    showBackButton: Boolean = true,
    onBack: () -> Unit,
) {
    var selectedTab by remember(call.id) { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        // ── TopAppBar ─────────────────────────────────────────────────────────
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = urlHost(call.url),
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val path = urlPath(call.url)
                    if (path.isNotEmpty()) {
                        Text(
                            text = path,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            },
            navigationIcon = {
                if (showBackButton) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            },
            actions = {
                MethodBadge(call.method, modifier = Modifier.padding(end = 12.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        )

        // ── Status summary strip ──────────────────────────────────────────────
        StatusSummaryStrip(call)

        // ── Tabs ──────────────────────────────────────────────────────────────
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Request") },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Response") },
            )
        }

        // ── Tab content ───────────────────────────────────────────────────────
        when (selectedTab) {
            0 -> RequestTab(call)
            1 -> ResponseTab(call)
        }
    }
}

// ── Status summary strip ──────────────────────────────────────────────────────

@Composable
private fun StatusSummaryStrip(call: NetworkCall) {
    val sidekickColors = LocalSidekickColors.current
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (call.status) {
                CallStatus.PENDING -> {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = sidekickColors.statusPending,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        "Pending…",
                        style = MaterialTheme.typography.labelMedium,
                        color = sidekickColors.statusPending,
                    )
                }
                CallStatus.ERROR -> {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        "Network Error",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    call.error?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                CallStatus.COMPLETE -> {
                    val code = call.responseCode ?: 0
                    val statusColor = when {
                        code < 300 -> sidekickColors.statusSuccess
                        code < 400 -> sidekickColors.statusRedirect
                        code < 500 -> sidekickColors.statusClientError
                        else       -> sidekickColors.statusServerError
                    }
                    Text(
                        text = "$code",
                        style = MaterialTheme.typography.titleMedium,
                        color = statusColor,
                        fontFamily = FontFamily.Monospace,
                    )
                    val label = statusText(code)
                    if (label.isNotEmpty()) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColor,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    call.durationMs?.let {
                        Text(
                            text = "${it}ms",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    call.responseBody?.let {
                        Text(
                            text = it.bodySizeLabel(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
    }
}

// ── Request tab ───────────────────────────────────────────────────────────────

@Composable
private fun RequestTab(call: NetworkCall) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DetailSection(label = "URL") {
            CopyableMonoBlock(call.url)
        }
        DetailSection(label = "Method") {
            MonoText(call.method)
        }
        if (call.requestHeaders.isNotEmpty()) {
            DetailSection(label = "Headers") {
                HeadersTable(call.requestHeaders)
            }
        }
        call.requestBody?.let {
            DetailSection(label = "Body") {
                CopyableCodeBlock(it.prettyPrintJson())
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ── Response tab ──────────────────────────────────────────────────────────────

@Composable
private fun ResponseTab(call: NetworkCall) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        call.responseCode?.let {
            DetailSection(label = "Status") {
                MonoText("$it ${statusText(it)}".trim())
            }
        }
        call.durationMs?.let {
            DetailSection(label = "Duration") {
                MonoText("${it}ms")
            }
        }
        if (call.responseHeaders.isNotEmpty()) {
            DetailSection(label = "Headers") {
                HeadersTable(call.responseHeaders)
            }
        }
        call.responseBody?.let {
            DetailSection(label = "Body") {
                CopyableCodeBlock(it.prettyPrintJson())
            }
        }
        call.error?.let {
            DetailSection(label = "Error") {
                MonoText(it, color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ── Shared sub-components ─────────────────────────────────────────────────────

@Composable
private fun DetailSection(label: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            content()
        }
    }
}

@Composable
private fun MonoText(
    text: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = color,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CopyableMonoBlock(text: String) {
    val clipboard = LocalClipboardManager.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(4.dp))
        IconButton(
            onClick = { clipboard.setText(AnnotatedString(text)) },
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CopyableCodeBlock(text: String) {
    val clipboard = LocalClipboardManager.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            // Copy button header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(
                    onClick = { clipboard.setText(AnnotatedString(text)) },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp),
            )
        }
    }
}

@Composable
private fun HeadersTable(headers: Map<String, String>) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            headers.entries.forEachIndexed { index, (key, value) ->
                val rowBg = if (index % 2 == 0)
                    MaterialTheme.colorScheme.surfaceContainerLowest
                else
                    MaterialTheme.colorScheme.surfaceContainer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(120.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (index < headers.size - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            }
        }
    }
}

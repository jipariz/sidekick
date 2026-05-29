package dev.parez.sidekick.network.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.parez.sidekick.network.CallStatus
import dev.parez.sidekick.network.NetworkCall
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * Detail pane for the Network Monitor. Used in single-pane and two-pane layouts where Request and
 * Response coexist in a single column driven by tabs. The three-pane / side-by-side layout uses
 * [NetworkCallDetailSplit] instead.
 *
 * @param showBackButton When true (compact mode) shows a back arrow in the TopAppBar. When false
 *   (two-pane) shows nothing.
 * @param onBack Called when the back arrow is tapped (compact) or when the pane should be
 *   dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NetworkCallDetailPane(
    call: NetworkCall,
    showBackButton: Boolean = true,
    onBack: () -> Unit,
) {
    var selectedTab by remember(call.id) { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
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
                actions = { MethodBadge(call.method, modifier = Modifier.padding(end = 12.dp)) },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
            )
        }
    ) {
        Column(Modifier.padding(it).fillMaxSize()) {
            StatusSummaryStrip(call)

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    text = { Text("Request") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    text = { Text("Response") },
                )
            }

            when (selectedTab) {
                0 -> RequestTab(call)
                1 -> ResponseTab(call)
            }
        }
    }
}

/**
 * Side-by-side Request + Response layout for wide windows, with a draggable splitter between the
 * two columns. Mirrors the M3 Adaptive list-pane behavior: three snap points (`0.3 / 0.5 / 0.7`)
 * with a hard min/max clamp of `0.2..0.8`, and an animated snap-back on release to the nearest
 * anchor.
 */
@Composable
internal fun NetworkCallDetailSplit(call: NetworkCall) {
    val snapPoints = remember { listOf(0.3f, 0.5f, 0.7f) }
    val proportion = remember { Animatable(0.5f) }
    val scope = rememberCoroutineScope()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxHeight().weight(proportion.value)) { NetworkCallRequestPane(call) }
            VerticalSplitterHandle(
                onDragDeltaPx = { delta ->
                    if (totalWidthPx > 0f) {
                        val next = (proportion.value + delta / totalWidthPx).coerceIn(0.2f, 0.8f)
                        scope.launch { proportion.snapTo(next) }
                    }
                },
                onDragStopped = {
                    val closest = snapPoints.minBy { abs(it - proportion.value) }
                    proportion.animateTo(closest)
                },
            )
            Box(Modifier.fillMaxHeight().weight(1f - proportion.value)) {
                NetworkCallResponsePane(call)
            }
        }
    }
}

@Composable
private fun VerticalSplitterHandle(
    onDragDeltaPx: (Float) -> Unit,
    onDragStopped: suspend () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier =
            Modifier.fillMaxHeight()
                .width(8.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta -> onDragDeltaPx(delta) },
                    interactionSource = interactionSource,
                    onDragStopped = { onDragStopped() },
                ),
        contentAlignment = Alignment.Center,
    ) {
        VerticalDragHandle(interactionSource = interactionSource)
    }
}

/**
 * Request half of the side-by-side layout. Mirrors [NetworkCallResponsePane] in shape (PaneIdentity
 * header + tab body) so the two read as paired panels — "sent" on the left, "received" on the
 * right.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NetworkCallRequestPane(call: NetworkCall) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    PaneIdentity(
                        icon = Icons.Default.CloudUpload,
                        iconContainer = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                        label = "Request",
                        primary = urlHost(call.url),
                        secondary = urlPath(call.url).takeIf { it.isNotEmpty() },
                    )
                },
                actions = { MethodBadge(call.method, modifier = Modifier.padding(end = 12.dp)) },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) { RequestTab(call) }
    }
}

/**
 * Header used by both the Request and Response panes. The tonal-tinted icon disc + small-caps label
 * give the pane a strong directional identity (sent vs received) before the user reads any of the
 * call detail.
 */
@Composable
private fun PaneIdentity(
    icon: ImageVector,
    iconContainer: Color,
    iconTint: Color,
    label: String,
    primary: String,
    secondary: String?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(color = iconContainer, shape = CircleShape, modifier = Modifier.size(36.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = primary,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (secondary != null) {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Supporting (extra) pane that shows only the response side of a call. Used by
 * `NetworkMonitorContent` on wide windows where the scaffold can fit three panes — list + detail
 * (Request) + extra (Response).
 *
 * Visually mirrors the Request pane's identity using the opposite direction (CloudDownload +
 * tertiary tonal accent) so the two panes read as "sent" and "received" without the user having to
 * scan their contents.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NetworkCallResponsePane(call: NetworkCall) {
    val primary =
        when (call.status) {
            CallStatus.PENDING -> "Pending"
            CallStatus.ERROR -> "Network error"
            CallStatus.COMPLETE ->
                call.responseCode?.let { code -> "$code ${statusText(code)}".trim() } ?: "—"
        }
    val secondary = call.durationMs?.let { "${it}ms" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    PaneIdentity(
                        icon = Icons.Default.CloudDownload,
                        iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                        iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                        label = "Response",
                        primary = primary,
                        secondary = secondary,
                    )
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Mirror the detail pane's status summary so the response pane is
            // self-sufficient on wide windows — duration, error message, and
            // (for errors) the error itself live here rather than next door.
            StatusSummaryStrip(call)
            ResponseTab(call)
        }
    }
}

// ── Status summary strip ──────────────────────────────────────────────────────

@Composable
private fun StatusSummaryStrip(call: NetworkCall) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (call.status) {
                CallStatus.PENDING -> {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        "Pending…",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outlineVariant,
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
                    val statusColor =
                        when {
                            code < 300 -> MaterialTheme.colorScheme.secondary
                            code < 400 -> MaterialTheme.colorScheme.primary
                            code < 500 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.error
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
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        DetailSection(label = "URL") { CopyableMonoBlock(call.url) }
        DetailSection(label = "Method") { MonoText(call.method) }
        if (call.requestHeaders.isNotEmpty()) {
            DetailSection(label = "Headers") { HeadersTable(call.requestHeaders) }
        }
        call.requestBody?.let {
            DetailSection(label = "Body") { CopyableCodeBlock(it.prettyPrintJson()) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ── Response tab ──────────────────────────────────────────────────────────────

@Composable
private fun ResponseTab(call: NetworkCall) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        call.responseCode?.let {
            DetailSection(label = "Status") { MonoText("$it ${statusText(it)}".trim()) }
        }
        call.durationMs?.let { DetailSection(label = "Duration") { MonoText("${it}ms") } }
        if (call.responseHeaders.isNotEmpty()) {
            DetailSection(label = "Headers") { HeadersTable(call.responseHeaders) }
        }
        call.responseBody?.let {
            DetailSection(label = "Body") { CopyableCodeBlock(it.prettyPrintJson()) }
        }
        call.error?.let {
            DetailSection(label = "Error") { MonoText(it, color = MaterialTheme.colorScheme.error) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ── Shared sub-components ─────────────────────────────────────────────────────

@Composable
private fun DetailSection(label: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
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
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { content() }
    }
}

@Composable
private fun MonoText(text: String, color: Color = MaterialTheme.colorScheme.onSurface) {
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
    // TODO: migrate to LocalClipboard once Compose Multiplatform ships a
    // commonMain ClipEntry text helper (1.11.0 only exposes the suspend
    // Clipboard.setClipEntry, with no per-platform ClipEntry factory in common).
    @Suppress("DEPRECATION") val clipboard = LocalClipboardManager.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { clipboard.setText(AnnotatedString(text)) }) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CopyableCodeBlock(text: String) {
    // TODO: migrate to LocalClipboard once Compose Multiplatform ships a
    // commonMain ClipEntry text helper (1.11.0 only exposes the suspend
    // Clipboard.setClipEntry, with no per-platform ClipEntry factory in common).
    @Suppress("DEPRECATION") val clipboard = LocalClipboardManager.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = { clipboard.setText(AnnotatedString(text)) }) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier =
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(12.dp),
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
                val rowBg =
                    if (index % 2 == 0) MaterialTheme.colorScheme.surfaceContainerLowest
                    else MaterialTheme.colorScheme.surfaceContainer
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
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
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

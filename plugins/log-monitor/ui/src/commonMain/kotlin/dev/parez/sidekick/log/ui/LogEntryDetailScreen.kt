package dev.parez.sidekick.log.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.parez.sidekick.log.LogEntry
import dev.parez.sidekick.log.LogLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LogEntryDetailPane(
    entry: LogEntry,
    showBackButton: Boolean = true,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Severity-tinted disc + LOG · LEVEL overline + tag/timestamp.
                    // Mirrors the network monitor's Request/Response pane identity
                    // — here driven by [LogLevel] instead of direction so the
                    // pane reads as "this is a WARN log from tag X" at a glance.
                    PaneIdentity(
                        level = entry.level,
                        primary = entry.tag,
                        secondary = formatTimestamp(entry.timestamp),
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = { LevelBadge(entry.level, modifier = Modifier.padding(end = 12.dp)) },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
            )
        }
    ) {
        Column(Modifier.padding(it).fillMaxSize()) {
            // -- Content ----------------------------------------------------------
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                DetailSection(label = "Tag") { CopyableMonoBlock(entry.tag) }
                DetailSection(label = "Message") { CopyableCodeBlock(entry.message) }
                entry.throwable?.let {
                    DetailSection(label = "Stacktrace") { CopyableCodeBlock(it) }
                }
                DetailSection(label = "Timestamp") { MonoText(formatTimestamp(entry.timestamp)) }
                entry.metadata?.let { meta ->
                    if (meta.isNotEmpty()) {
                        DetailSection(label = "Metadata") { MetadataTable(meta) }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// -- Pane identity ------------------------------------------------------------

/**
 * Header inside the detail [TopAppBar]: a severity-tinted disc carrying the level's icon, plus a
 * small-caps "LOG · LEVEL" overline above the tag and timestamp. Mirrors the network monitor's
 * request/response identity so the two plugins feel like one design system.
 */
@Composable
private fun PaneIdentity(level: LogLevel, primary: String, secondary: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            color = level.containerColor(),
            shape = CircleShape,
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = level.icon(),
                    contentDescription = null,
                    tint = level.onContainerColor(),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column {
            Text(
                text = "LOG · ${level.fullLabel().uppercase()}",
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

// -- Shared sub-components ----------------------------------------------------

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
private fun MetadataTable(metadata: Map<String, String>) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            metadata.entries.forEachIndexed { index, (key, value) ->
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
                        modifier = Modifier.weight(0.4f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(0.6f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (index < metadata.size - 1) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

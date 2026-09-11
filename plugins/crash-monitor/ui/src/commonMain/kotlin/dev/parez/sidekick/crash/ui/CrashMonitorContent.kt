package dev.parez.sidekick.crash.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.parez.sidekick.crash.CrashRecord
import dev.parez.sidekick.plugin.SidekickShare

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CrashMonitorContent(
    crashes: List<CrashRecord>,
    selected: CrashRecord?,
    onSelect: (String?) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = selected?.exceptionType ?: "Crashes",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (selected != null) onSelect(null) else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val target = selected
                    if (target != null) {
                        IconButton(
                            onClick = {
                                SidekickShare.share(
                                    target.toShareText(),
                                    subject = "sidekick-crash",
                                )
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share crash")
                        }
                        CrashBadge(target, modifier = Modifier.padding(end = 12.dp))
                    } else {
                        IconButton(
                            onClick = {
                                SidekickShare.share(
                                    crashes.toShareText(),
                                    subject = "sidekick-crashes",
                                )
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Export all")
                        }
                        IconButton(onClick = onClear) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear all")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (selected != null) {
                CrashDetailPane(crash = selected)
            } else {
                CrashListPane(crashes = crashes, onSelect = onSelect)
            }
        }
    }
}

@Composable
private fun CrashListPane(
    crashes: List<CrashRecord>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (crashes.isEmpty()) {
        CrashEmptyState(
            icon = Icons.Default.ReportProblem,
            title = "No crashes recorded",
            description = "Anything that escapes after CrashMonitor.install() shows up here.",
            modifier = modifier,
        )
        return
    }
    // Newest first — the crash you are chasing is the one that just happened.
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(items = crashes.asReversed(), key = { it.id }, contentType = { "crash" }) { crash ->
            CrashRow(crash = crash, onClick = { onSelect(crash.id) })
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@Composable
private fun CrashRow(crash: CrashRecord, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CrashIdentityDisc(
            icon = crash.icon(),
            containerColor = crash.containerColor(),
            contentColor = crash.onContainerColor(),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = crash.exceptionType,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (crash.message.isNotBlank()) {
                Text(
                    text = crash.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text =
                    "${formatTimestamp(crash.timestamp)} · ${crash.origin} · " +
                        "thread ${crash.threadName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                fontFamily = FontFamily.Monospace,
            )
        }
        CrashBadge(crash)
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun CrashDetailPane(crash: CrashRecord, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Pane-identity header, matching the log monitor's detail header.
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CrashIdentityDisc(
                icon = crash.icon(),
                containerColor = crash.containerColor(),
                contentColor = crash.onContainerColor(),
            )
            Column {
                Text(
                    text = "CRASH · ${crash.severityLabel()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = crash.exceptionType,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text =
                        "${formatTimestamp(crash.timestamp)} · ${crash.origin} · " +
                            "thread ${crash.threadName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (crash.message.isNotBlank()) {
            CrashSection(label = "Message") {
                Text(
                    text = crash.message,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        CrashSection(label = "Stack trace") {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    crash.frames.forEach { frame ->
                        Text(
                            text = frame.text,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            // Framework frames recede so the app's own frames are
                            // findable at a glance — the whole point of isAppFrame.
                            color =
                                if (frame.isAppFrame) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Section header shared with the network and log detail panes: primary uppercase label + rule. */
@Composable
private fun CrashSection(label: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { content() }
    }
}

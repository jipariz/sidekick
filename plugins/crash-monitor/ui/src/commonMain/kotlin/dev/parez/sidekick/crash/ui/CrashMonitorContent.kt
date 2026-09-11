package dev.parez.sidekick.crash.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
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
                        modifier = Modifier.padding(start = 8.dp),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (selected != null) onSelect(null) else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val shareTarget = selected
                    if (shareTarget != null) {
                        IconButton(
                            onClick = {
                                SidekickShare.share(
                                    shareTarget.toShareText(),
                                    subject = "sidekick-crash",
                                )
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share crash")
                        }
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
        Text(
            text =
                "No crashes recorded. Call CrashMonitor.install() during startup; anything that " +
                    "escapes after that shows up here, including on the run after the crash.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.fillMaxWidth().padding(24.dp),
        )
        return
    }
    // Newest first — the crash you are chasing is the one that just happened.
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(items = crashes.asReversed(), key = { it.id }, contentType = { "crash" }) { crash ->
            ListItem(
                headlineContent = { Text(crash.title, maxLines = 2) },
                supportingContent = {
                    val kind = if (crash.fatal) "fatal" else "non-fatal"
                    Text("$kind · ${crash.origin} · ${crash.threadName}")
                },
                modifier = Modifier.fillMaxWidth().clickable { onSelect(crash.id) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@Composable
private fun CrashDetailPane(crash: CrashRecord, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(text = crash.title, style = MaterialTheme.typography.titleMedium)
        Text(
            text =
                "${if (crash.fatal) "Fatal" else "Non-fatal"} · ${crash.origin} · " +
                    "thread ${crash.threadName}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        crash.frames.forEach { frame ->
            Text(
                text = frame.text,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                // Framework frames are dimmed so the app's own frames are findable
                // at a glance — the whole point of tracking isAppFrame.
                color =
                    if (frame.isAppFrame) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
            )
        }
    }
}

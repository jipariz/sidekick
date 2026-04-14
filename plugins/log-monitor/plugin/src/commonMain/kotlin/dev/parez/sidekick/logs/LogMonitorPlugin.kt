package dev.parez.sidekick.logs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import dev.parez.sidekick.logs.ui.LogMonitorContent
import dev.parez.sidekick.plugin.SidekickPlugin
import kotlinx.coroutines.launch

class LogMonitorPlugin(
    val store: LogMonitorStore = LogMonitorStore,
    retentionMs: Long = RetentionPeriod.ONE_HOUR,
) : SidekickPlugin {

    init {
        store.init(retentionMs)
    }

    override val id: String = "log-monitor"
    override val title: String = "Logs"
    override val icon: ImageVector = Icons.AutoMirrored.Default.List

    @Composable
    override fun Content() {
        val entries by store.entries().collectAsState(emptyList())
        var selected by remember { mutableStateOf<LogEntry?>(null) }
        val scope = rememberCoroutineScope()

        LogMonitorContent(
            entries = entries,
            selected = selected,
            onSelect = { selected = it },
            onClear = { scope.launch { store.clear() } },
            onBack = { selected = null },
        )
    }
}

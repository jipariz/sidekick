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
import dev.parez.sidekick.plugin.LocalSidekickBackNavigator
import dev.parez.sidekick.plugin.SidekickPlugin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.launch

class LogMonitorPlugin(
    val store: LogMonitorStore = LogMonitorStore,
    retentionPeriod: Duration = 1.hours,
) : SidekickPlugin {

    init {
        store.init(retentionPeriod)
    }

    override val id: String = "log-monitor"
    override val title: String = "Logs"
    override val icon: ImageVector = Icons.AutoMirrored.Default.List

    @Composable
    override fun Content() {
        val navigateBack = LocalSidekickBackNavigator.current
        val entries by store.entries.collectAsState(emptyList())
        var selected by remember { mutableStateOf<LogEntry?>(null) }
        val scope = rememberCoroutineScope()

        LogMonitorContent(
            entries = entries,
            selected = selected,
            onSelect = { selected = it },
            onClear = { scope.launch { store.clear() } },
            onBack = navigateBack,
        )
    }
}

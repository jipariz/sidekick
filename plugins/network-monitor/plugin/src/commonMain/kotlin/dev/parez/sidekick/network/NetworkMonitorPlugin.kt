package dev.parez.sidekick.network

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import dev.parez.sidekick.network.ui.NetworkMonitorContent
import dev.parez.sidekick.plugin.SidekickPlugin
import kotlin.time.Duration
import kotlinx.coroutines.launch

class NetworkMonitorPlugin(
    private val store: NetworkMonitorStore = NetworkMonitorStore,
    retentionPeriod: Duration = RetentionPeriod.ONE_HOUR,
) : SidekickPlugin {

    init {
        store.init(retentionPeriod)
    }

    override val id: String = "network-monitor"
    override val title: String = "Network"
    override val icon: ImageVector = Icons.Default.NetworkCheck

    @Composable
    override fun Content() {
        val calls by store.calls.collectAsState(emptyList())
        var selected by remember { mutableStateOf<NetworkCall?>(null) }
        val scope = rememberCoroutineScope()

        NetworkMonitorContent(
            calls = calls,
            selected = selected,
            onSelect = { selected = it },
            onClear = { scope.launch { store.clear() } },
            onBack = { selected = null },
        )
    }
}

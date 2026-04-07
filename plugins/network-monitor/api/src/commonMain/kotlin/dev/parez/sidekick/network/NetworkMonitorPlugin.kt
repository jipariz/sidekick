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
import dev.parez.sidekick.network.ui.NetworkCallDetailScreen
import dev.parez.sidekick.network.ui.NetworkCallListScreen
import dev.parez.sidekick.plugin.SidekickPlugin
import kotlinx.coroutines.launch

class NetworkMonitorPlugin(
    private val store: NetworkMonitorStore = NetworkMonitorStore,
    retentionMs: Long = RetentionPeriod.ONE_HOUR,
) : SidekickPlugin {

    init {
        store.init(retentionMs)
    }

    override val id: String = "network-monitor"
    override val title: String = "Network"
    override val icon: ImageVector = Icons.Default.NetworkCheck

    @Composable
    override fun Content() {
        val calls by store.calls().collectAsState(emptyList())
        var selected by remember { mutableStateOf<NetworkCall?>(null) }
        val scope = rememberCoroutineScope()

        if (selected == null) {
            NetworkCallListScreen(
                calls = calls,
                onSelect = { selected = it },
                onClear = { scope.launch { store.clear() } },
            )
        } else {
            NetworkCallDetailScreen(call = selected!!, onBack = { selected = null })
        }
    }
}

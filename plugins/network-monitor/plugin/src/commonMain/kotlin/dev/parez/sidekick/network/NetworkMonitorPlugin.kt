package dev.parez.sidekick.network

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import dev.parez.sidekick.network.ui.NetworkMonitorContent
import dev.parez.sidekick.plugin.LocalSidekickNavigator
import dev.parez.sidekick.plugin.SidekickNavigator
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
        val navigator = LocalSidekickNavigator.current

        // Consume deep link once on entry — store the target call ID
        var pendingCallId by remember {
            mutableStateOf(navigator.consumeDeepLink())
        }

        // Resolve pending call ID to a selected call when calls are available
        LaunchedEffect(calls, pendingCallId) {
            val targetId = pendingCallId ?: return@LaunchedEffect
            val call = calls.firstOrNull { it.id == targetId }
            if (call != null) {
                selected = call
                pendingCallId = null
            }
        }

        NetworkMonitorContent(
            calls = calls,
            selected = selected,
            onSelect = { selected = it },
            onClear = { scope.launch { store.clear() } },
            onBack = { selected = null },
        )
    }
}

package dev.parez.sidekick.network

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.parez.sidekick.network.di.NetworkMonitorKoinContext
import dev.parez.sidekick.network.di.networkMonitorViewModelModule
import dev.parez.sidekick.network.ui.NetworkMonitorContent
import dev.parez.sidekick.plugin.SidekickPlugin
import kotlin.time.Duration
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.viewmodel.koinViewModel

class NetworkMonitorPlugin(
    retentionPeriod: Duration = RetentionPeriod.ONE_HOUR,
) : SidekickPlugin {

    init {
        NetworkMonitorKoinContext.loadViewModelModule(networkMonitorViewModelModule)
        NetworkMonitorKoinContext.koin.get<NetworkMonitorStore>().init(retentionPeriod)
    }

    override val id: String = "network-monitor"
    override val title: String = "Network"
    override val icon: ImageVector = Icons.Default.NetworkCheck

    @Composable
    override fun Content(navigateBackToList: () -> Unit) {
        KoinIsolatedContext(context = NetworkMonitorKoinContext.koinApp) {
            val viewModel: NetworkMonitorViewModel = koinViewModel()
            val calls by viewModel.calls.collectAsStateWithLifecycle()

            NetworkMonitorContent(
                calls = calls,
                selected = viewModel.selected,
                onSelect = viewModel::select,
                onClear = viewModel::clear,
                onBack = navigateBackToList,
            )
        }
    }
}

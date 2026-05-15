package dev.parez.sidekick.network

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import dev.parez.sidekick.network.di.NetworkMonitorKoinContext
import dev.parez.sidekick.network.di.networkMonitorViewModelModule
import dev.parez.sidekick.network.ui.NetworkMonitorContent
import dev.parez.sidekick.plugin.LocalSidekickBackNavigator
import dev.parez.sidekick.plugin.SidekickPlugin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.viewmodel.koinViewModel

class NetworkMonitorPlugin(
    retentionPeriod: Duration = 1.hours,
) : SidekickPlugin {

    init {
        NetworkMonitorKoinContext.loadViewModelModule(networkMonitorViewModelModule)
        NetworkMonitorKoinContext.getDefaultStore().init(retentionPeriod)
    }

    override val id: String = "network-monitor"
    override val title: String = "Network"
    override val icon: ImageVector = Icons.Default.NetworkCheck

    @Composable
    override fun Content() {
        val navigateBack = LocalSidekickBackNavigator.current
        KoinIsolatedContext(context = NetworkMonitorKoinContext.koinApp) {
            val viewModel: NetworkMonitorViewModel = koinViewModel()
            val lazyItems = viewModel.pagedCalls.collectAsLazyPagingItems()
            val selected by viewModel.selectedCall.collectAsStateWithLifecycle()
            val query by viewModel.query.collectAsStateWithLifecycle()
            val methodFilter by viewModel.methodFilter.collectAsStateWithLifecycle()
            val filteredCount by viewModel.filteredCount.collectAsStateWithLifecycle()

            NetworkMonitorContent(
                lazyItems = lazyItems,
                selected = selected,
                query = query,
                methodFilter = methodFilter,
                filteredCount = filteredCount,
                onSelect = viewModel::select,
                onQueryChange = viewModel::setQuery,
                onToggleMethod = viewModel::toggleMethod,
                onClear = viewModel::clear,
                onBack = navigateBack,
            )
        }
    }
}

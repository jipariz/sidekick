package dev.parez.sidekick.network

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import dev.parez.sidekick.network.di.NetworkMonitorKoinContext
import dev.parez.sidekick.network.di.networkMonitorViewModelModule
import dev.parez.sidekick.network.ui.NetworkMonitorContent
import dev.parez.sidekick.plugin.LocalSidekickBackNavigator
import dev.parez.sidekick.plugin.SidekickBadged
import dev.parez.sidekick.plugin.SidekickLifecycleAware
import dev.parez.sidekick.plugin.SidekickPlugin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.viewmodel.koinViewModel

class NetworkMonitorPlugin(
    retentionPeriod: Duration = 1.hours,
    bodyBudgetChars: Int = BodyBudget.Default,
) : SidekickPlugin, SidekickBadged, SidekickLifecycleAware {

    private val _badge = mutableStateOf<Int?>(null)

    /** Calls recorded since the user last opened this plugin. */
    override val badge: State<Int?> = _badge

    // Total at the moment the plugin was last opened. Everything past it is unread.
    private var seenCount = 0L
    private var totalCount = 0L

    init {
        NetworkMonitorKoinContext.loadViewModelModule(networkMonitorViewModelModule)
        val store = NetworkMonitorKoinContext.getDefaultStore()
        store.init(retentionPeriod, bodyBudgetChars)

        // Unfiltered count — the badge reflects everything captured, not whatever
        // filter the list screen happens to have applied.
        NetworkMonitorKoinContext.storeScope().launch {
            store.filteredCount(flowOf(NetworkFilter())).collect { total ->
                totalCount = total
                // The store trims old rows, so the total can fall. Re-baseline when it
                // does, otherwise the badge would stay stuck at zero afterwards.
                if (total < seenCount) seenCount = total
                _badge.value = (total - seenCount).takeIf { it > 0 }?.toInt()
            }
        }
    }

    override fun onAttach() {
        seenCount = totalCount
        _badge.value = null
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
                onShare = viewModel::shareAll,
                onBack = navigateBack,
            )
        }
    }
}

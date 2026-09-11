package dev.parez.sidekick.log

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import dev.parez.sidekick.log.di.LogMonitorKoinContext
import dev.parez.sidekick.log.di.logMonitorViewModelModule
import dev.parez.sidekick.log.ui.LogMonitorContent
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

public class LogMonitorPlugin(retentionPeriod: Duration = 1.hours) :
    SidekickPlugin, SidekickBadged, SidekickLifecycleAware {

    private val _badge = mutableStateOf<Int?>(null)

    /** Entries recorded since the user last opened this plugin. */
    override val badge: State<Int?> = _badge

    // Total at the moment the plugin was last opened. Everything past it is unread.
    private var seenCount = 0L
    private var totalCount = 0L

    init {
        LogMonitorKoinContext.loadViewModelModule(logMonitorViewModelModule)
        val store = LogMonitorKoinContext.getDefaultStore()
        store.init(retentionPeriod)

        // Unfiltered count — the badge reflects everything captured, not whatever
        // filter the list screen happens to have applied.
        LogMonitorKoinContext.storeScope().launch {
            store.filteredCount(flowOf(LogFilter())).collect { total ->
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

    override val id: String = "log-monitor"
    override val title: String = "Logs"
    override val icon: ImageVector = Icons.AutoMirrored.Default.List

    @Composable
    override fun Content() {
        val navigateBack = LocalSidekickBackNavigator.current
        KoinIsolatedContext(context = LogMonitorKoinContext.koinApp) {
            val viewModel: LogMonitorViewModel = koinViewModel()
            val lazyItems = viewModel.pagedEntries.collectAsLazyPagingItems()
            val selected by viewModel.selectedEntry.collectAsStateWithLifecycle()
            val query by viewModel.query.collectAsStateWithLifecycle()
            val levelFilter by viewModel.levelFilter.collectAsStateWithLifecycle()
            val filteredCount by viewModel.filteredCount.collectAsStateWithLifecycle()

            LogMonitorContent(
                lazyItems = lazyItems,
                selected = selected,
                query = query,
                levelFilter = levelFilter,
                filteredCount = filteredCount,
                onSelect = viewModel::select,
                onQueryChange = viewModel::setQuery,
                onToggleLevel = viewModel::toggleLevel,
                onClear = viewModel::clear,
                onShare = viewModel::shareAll,
                onBack = navigateBack,
            )
        }
    }
}

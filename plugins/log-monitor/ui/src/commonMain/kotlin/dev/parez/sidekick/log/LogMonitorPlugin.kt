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
import kotlinx.coroutines.launch
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.viewmodel.koinViewModel

public class LogMonitorPlugin(retentionPeriod: Duration = 1.hours) :
    SidekickPlugin, SidekickBadged, SidekickLifecycleAware {

    private val _badge = mutableStateOf<Int?>(null)

    /** Entries recorded since the user last opened this plugin. */
    override val badge: State<Int?> = _badge

    // Sequence position at the moment the plugin was last opened. Everything past it
    // is unread. `seen` stays null until the first value arrives, so attaching before
    // the store has reported anything cannot baseline against a phantom zero and mark
    // already-visible restored rows as new.
    private var seen: Long? = null
    private var total = 0L

    init {
        LogMonitorKoinContext.loadViewModelModule(logMonitorViewModelModule)
        val store = LogMonitorKoinContext.getDefaultStore()
        store.init(retentionPeriod)

        // Unfiltered count — the badge reflects everything captured, not whatever
        // filter the list screen happens to have applied.
        LogMonitorKoinContext.storeScope().launch {
            store.recordedCount.collect { recorded ->
                total = recorded
                val baseline = seen ?: recorded.also { seen = it }
                _badge.value = (recorded - baseline).takeIf { it > 0 }?.toInt()
            }
        }
    }

    override fun onAttach() {
        seen = total
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

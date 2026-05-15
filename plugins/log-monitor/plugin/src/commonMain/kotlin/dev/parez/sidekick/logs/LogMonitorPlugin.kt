package dev.parez.sidekick.logs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import dev.parez.sidekick.logs.di.LogMonitorKoinContext
import dev.parez.sidekick.logs.di.logMonitorViewModelModule
import dev.parez.sidekick.logs.ui.LogMonitorContent
import dev.parez.sidekick.plugin.LocalSidekickBackNavigator
import dev.parez.sidekick.plugin.SidekickPlugin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.viewmodel.koinViewModel

class LogMonitorPlugin(
    retentionPeriod: Duration = 1.hours,
) : SidekickPlugin {

    init {
        LogMonitorKoinContext.loadViewModelModule(logMonitorViewModelModule)
        LogMonitorKoinContext.getDefaultStore().init(retentionPeriod)
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
                onBack = navigateBack,
            )
        }
    }
}

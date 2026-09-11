package dev.parez.sidekick.crash

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.parez.sidekick.crash.di.CrashMonitorKoinContext
import dev.parez.sidekick.crash.di.crashMonitorViewModelModule
import dev.parez.sidekick.crash.ui.CrashMonitorContent
import dev.parez.sidekick.plugin.LocalSidekickBackNavigator
import dev.parez.sidekick.plugin.SidekickBadged
import dev.parez.sidekick.plugin.SidekickLifecycleAware
import dev.parez.sidekick.plugin.SidekickPlugin
import kotlinx.coroutines.launch
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.viewmodel.koinViewModel

/**
 * Shows crashes captured by [CrashMonitor], including ones from previous runs.
 *
 * Install the handler separately and early — ideally the first thing in `Application.onCreate` or
 * its equivalent — so it is in place before anything can fail:
 * ```kotlin
 * CrashMonitor.install(appPackagePrefix = "com.acme.app")
 * ```
 *
 * Constructing this plugin does not install the handler: plugins are created when the overlay is
 * first composed, which is far too late to catch a startup crash.
 */
public class CrashMonitorPlugin : SidekickPlugin, SidekickBadged, SidekickLifecycleAware {

    private val _badge = mutableStateOf<Int?>(null)

    /** Crashes recorded since the user last opened this plugin. */
    override val badge: State<Int?> = _badge

    private var seenCount = 0
    private var totalCount = 0

    init {
        CrashMonitorKoinContext.loadViewModelModule(crashMonitorViewModelModule)
        val store = CrashMonitorKoinContext.getDefaultStore()
        CrashMonitorKoinContext.storeScope().launch {
            store.crashes.collect { list ->
                totalCount = list.size
                if (list.size < seenCount) seenCount = list.size
                _badge.value = (list.size - seenCount).takeIf { it > 0 }
            }
        }
    }

    override val id: String = "sidekick.crash-monitor"
    override val title: String = "Crashes"
    override val icon: ImageVector = Icons.Default.ReportProblem

    override fun onAttach() {
        seenCount = totalCount
        _badge.value = null
    }

    @Composable
    override fun Content() {
        val navigateBack = LocalSidekickBackNavigator.current
        KoinIsolatedContext(context = CrashMonitorKoinContext.koinApp) {
            val viewModel: CrashMonitorViewModel = koinViewModel()
            val crashes by viewModel.crashes.collectAsStateWithLifecycle()
            val selected by viewModel.selected.collectAsStateWithLifecycle()

            CrashMonitorContent(
                crashes = crashes,
                selected = selected,
                onSelect = viewModel::select,
                onClear = viewModel::clear,
                onBack = navigateBack,
            )
        }
    }
}

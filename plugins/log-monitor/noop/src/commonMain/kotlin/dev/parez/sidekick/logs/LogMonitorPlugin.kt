package dev.parez.sidekick.logs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import dev.parez.sidekick.plugin.SidekickPlugin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Release-variant stub for `LogMonitorPlugin`. Constructor is a no-op — does not
 * initialise any Koin graph, store, or SQLDelight database. `Content()` renders
 * nothing; production builds typically pair this with `core/noop`'s `Sidekick()`
 * so the panel is never composed anyway.
 */
@Suppress("UNUSED_PARAMETER")
class LogMonitorPlugin(
    retentionPeriod: Duration = 1.hours,
) : SidekickPlugin {

    override val id: String = "log-monitor"
    override val title: String = "Logs"
    override val icon: ImageVector = Icons.AutoMirrored.Default.List

    @Composable
    override fun Content() = Unit
}

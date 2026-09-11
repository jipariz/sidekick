package dev.parez.sidekick.log

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import dev.parez.sidekick.plugin.SidekickBadged
import dev.parez.sidekick.plugin.SidekickPlugin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Release-variant stub for `LogMonitorPlugin`. Constructor is a no-op — does not initialise any
 * Koin graph, store, or SQLDelight database. `Content()` renders nothing; production builds
 * typically pair this with `core/noop`'s `Sidekick()` so the panel is never composed anyway.
 */
@Suppress("UNUSED_PARAMETER")
public class LogMonitorPlugin(retentionPeriod: Duration = 1.hours) :
    SidekickPlugin, SidekickBadged {

    /** Always null — the noop variant records nothing, so there is never anything unread. */
    override val badge: State<Int?> = mutableStateOf(null)

    override val id: String = "log-monitor"
    override val title: String = "Logs"
    override val icon: ImageVector = Icons.AutoMirrored.Default.List

    @Composable override fun Content(): Unit = Unit
}

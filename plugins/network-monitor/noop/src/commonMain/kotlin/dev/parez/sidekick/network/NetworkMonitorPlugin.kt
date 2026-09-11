package dev.parez.sidekick.network

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import dev.parez.sidekick.plugin.SidekickBadged
import dev.parez.sidekick.plugin.SidekickPlugin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Release-variant stub for `NetworkMonitorPlugin`. Constructor is a no-op — does not initialise any
 * Koin graph, store, or SQLDelight database. `Content()` renders nothing; production builds
 * typically pair this with `core/noop`'s `Sidekick()` so the panel is never composed anyway.
 */
@Suppress("UNUSED_PARAMETER")
public class NetworkMonitorPlugin(
    retentionPeriod: Duration = 1.hours,
    bodyBudgetChars: Int = BodyBudget.Default,
) : SidekickPlugin, SidekickBadged {

    /** Always null — the noop variant records nothing, so there is never anything unread. */
    override val badge: State<Int?> = mutableStateOf(null)

    override val id: String = "network-monitor"
    override val title: String = "Network"
    override val icon: ImageVector = Icons.Default.NetworkCheck

    @Composable override fun Content(): Unit = Unit
}

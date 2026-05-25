package dev.parez.sidekick.demo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.parez.sidekick.Sidekick
import dev.parez.sidekick.plugin.SidekickAppInfo
import dev.parez.sidekick.plugin.SidekickPlugin
import dev.parez.sidekick.plugin.rememberSidekickAppInfo

/**
 * Example host-side wrapper around [Sidekick]: a bottom-end FAB that toggles overlay visibility, an
 * [AnimatedVisibility] transition, and a close button in the overlay's app-bar actions slot. The
 * host's [content] renders behind the overlay.
 *
 * **This is sample code, not part of the Sidekick SDK.** Sidekick deliberately ships only the
 * [Sidekick] composable — the host owns the FAB, visibility state, and any trigger gesture. Copy
 * this file into your own app as a starting point, then customize the trigger / animation /
 * placement to taste. The composeApp's main entry point ([DemoApp]) shows a richer variant with a
 * first-launch Reveal hint and slide+fade transitions.
 *
 * ```kotlin
 * @Composable
 * fun App() {
 *     val plugins = remember {
 *         listOf(
 *             AppPreferencesPlugin(),
 *             NetworkMonitorPlugin(),
 *             LogMonitorPlugin(),
 *         )
 *     }
 *     MaterialTheme(/* host's color scheme */) {
 *         SidekickShell(plugins = plugins) {
 *             YourAppScreen()
 *         }
 *     }
 * }
 * ```
 *
 * @param plugins Plugins to show in the debug panel.
 * @param appInfo Optional host-app metadata shown in the panel header.
 * @param useSidekickTheme When true, the overlay applies its own Material 3 color scheme. Default
 *   `false` — most hosts want the overlay to inherit their app's
 *   [androidx.compose.material3.MaterialTheme].
 * @param content The app content rendered behind the overlay.
 */
@Composable
fun SidekickShell(
    plugins: List<SidekickPlugin>,
    appInfo: SidekickAppInfo? = rememberSidekickAppInfo(),
    useSidekickTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        if (!visible) {
            SmallFloatingActionButton(
                onClick = { visible = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                Icon(Icons.Default.BugReport, contentDescription = "Open Sidekick")
            }
        }
        AnimatedVisibility(visible = visible) {
            Sidekick(
                plugins = plugins,
                appInfo = appInfo,
                useSidekickTheme = useSidekickTheme,
                actions = {
                    IconButton(onClick = { visible = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close Sidekick")
                    }
                },
            )
        }
    }
}

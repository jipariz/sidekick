package dev.parez.sidekick

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
import dev.parez.sidekick.plugin.SidekickAppInfo
import dev.parez.sidekick.plugin.SidekickPlugin
import dev.parez.sidekick.plugin.rememberSidekickAppInfo

/**
 * Convenience wrapper that bundles the common Sidekick host-side wiring: a
 * bottom-end FAB that toggles overlay visibility, an [AnimatedVisibility]
 * transition, and a close button in the overlay's app-bar actions slot. The
 * host's [content] is rendered behind the overlay.
 *
 * Use this for the 80% case. If you need a different trigger (shake gesture,
 * keyboard shortcut, programmatic open), call [Sidekick] directly and manage
 * the visibility state yourself — see the demo-app for the explicit pattern.
 *
 * In release builds (when the consumer uses `releaseImplementation(noop)`),
 * `SidekickShell` collapses to `content()` — no FAB, no overlay, zero cost.
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
 * @param plugins           Plugins to show in the debug panel.
 * @param appInfo           Optional host-app metadata shown in the panel header.
 * @param useSidekickTheme  When true, the overlay applies its own Material 3 color
 *                          scheme. Default `false` — most hosts want the overlay
 *                          to inherit their app's [androidx.compose.material3.MaterialTheme].
 * @param content           The app content rendered behind the overlay.
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
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
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

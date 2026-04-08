package dev.parez.sidekick

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.parez.sidekick.plugin.SidekickColors
import dev.parez.sidekick.plugin.SidekickPlugin
import dev.parez.sidekick.ui.SidekickMenu
import dev.parez.sidekick.ui.theme.LocalSidekickThemeActive
import dev.parez.sidekick.ui.theme.SidekickDefaultColorScheme
import dev.parez.sidekick.ui.theme.SidekickTheme
import dev.parez.sidekick.ui.theme.isM3Default

/**
 * Wraps [content] and overlays the Sidekick debug panel (FAB + panel) on top.
 *
 * ### Theme inheritance
 * The overlay automatically inherits the host app's [MaterialTheme]:
 * - **Host has a custom MaterialTheme** → Sidekick's FAB and panel use those colors.
 * - **Host has no MaterialTheme** (or uses M3's unmodified default) → [SidekickDefaultColorScheme]
 *   (dark indigo) is applied as a fallback so the overlay always looks intentional.
 * - **[SidekickTheme] is applied above** → that theme wins unconditionally.
 *
 * The host's own content is rendered **outside** any Sidekick-owned MaterialTheme, so it is
 * never affected by Sidekick's colors.
 *
 * ### Overriding semantic colors
 * Pass [sidekickColors] to override HTTP-method badge and status-chip colors without changing
 * the Material color scheme:
 * ```kotlin
 * SidekickShell(
 *     plugins = plugins,
 *     sidekickColors = sidekickColors(httpDelete = Color.Red),
 * ) { MyAppContent() }
 * ```
 *
 * ### Forcing a specific theme
 * Wrap with [SidekickTheme] to bypass auto-detection entirely:
 * ```kotlin
 * SidekickTheme(colorScheme = myColorScheme) {
 *     SidekickShell(plugins) { MyAppContent() }
 * }
 * ```
 *
 * @param plugins        Plugins to show in the debug panel.
 * @param state          Optional state; defaults to [rememberSidekickState].
 * @param sidekickColors Sidekick-specific semantic colors (HTTP badges, status chips).
 *                       When null they are auto-derived from the resolved [MaterialTheme].
 * @param content        The host application content rendered beneath the overlay.
 */
@Composable
fun SidekickShell(
    plugins: List<SidekickPlugin>,
    state: SidekickState = rememberSidekickState(plugins),
    sidekickColors: SidekickColors? = null,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        content()

        // Resolve which color scheme to apply to the overlay:
        //  1. SidekickTheme already active above us → inherit it (no re-wrapping needed).
        //  2. Host has a custom MaterialTheme → inherit those colors.
        //  3. No custom MaterialTheme detected → fall back to SidekickDefaultColorScheme.
        val sidekickThemeActive = LocalSidekickThemeActive.current
        val hostScheme = MaterialTheme.colorScheme
        val overlayScheme = when {
            sidekickThemeActive -> hostScheme                  // explicit SidekickTheme above us
            !hostScheme.isM3Default() -> hostScheme            // host has a real MaterialTheme
            else -> SidekickDefaultColorScheme                 // no custom theme → use fallback
        }

        SidekickTheme(colorScheme = overlayScheme, sidekickColors = sidekickColors) {
            SmallFloatingActionButton(
                onClick = { state.open() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(Icons.Filled.BugReport, contentDescription = "Open Sidekick")
            }
            if (state.isOpen) {
                SidekickMenu(state)
            }
        }
    }
}

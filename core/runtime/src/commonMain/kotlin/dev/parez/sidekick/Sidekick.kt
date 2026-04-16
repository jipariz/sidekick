package dev.parez.sidekick

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import dev.parez.sidekick.plugin.SidekickAppInfo
import dev.parez.sidekick.plugin.SidekickColors
import dev.parez.sidekick.plugin.SidekickPlugin
import dev.parez.sidekick.plugin.rememberSidekickAppInfo
import dev.parez.sidekick.ui.SidekickMenu
import dev.parez.sidekick.ui.theme.LocalSidekickThemeActive
import dev.parez.sidekick.ui.theme.SidekickDefaultColorScheme
import dev.parez.sidekick.ui.theme.SidekickTheme
import dev.parez.sidekick.ui.theme.isM3Default

/**
 * Sidekick debug menu composable.
 *
 * Renders the full-screen debug panel with plugin list / plugin detail navigation.
 * The **client** is responsible for visibility (e.g. FAB + `AnimatedVisibility`); this
 * composable only renders the menu content itself.
 *
 * ### Theme inheritance
 * The menu automatically inherits the host app's [MaterialTheme]:
 * - **Host has a custom MaterialTheme** → Sidekick uses those colors.
 * - **Host has no MaterialTheme** (or uses M3's unmodified default) → [SidekickDefaultColorScheme]
 *   (dark indigo) is applied as a fallback.
 * - **[SidekickTheme] is applied above** → that theme wins unconditionally.
 *
 * @param plugins        Plugins to show in the debug panel.
 * @param onClose        Called when the user taps the close button.
 * @param appInfo        Optional host-app metadata shown in the panel header.
 * @param state          Optional state; defaults to [rememberSidekickState].
 * @param sidekickColors Sidekick-specific semantic colors (HTTP badges, status chips).
 */
@Composable
fun Sidekick(
    plugins: List<SidekickPlugin>,
    onClose: () -> Unit,
    appInfo: SidekickAppInfo? = rememberSidekickAppInfo(),
    state: SidekickState = rememberSidekickState(plugins),
    sidekickColors: SidekickColors? = null,
) {
    val sidekickThemeActive = LocalSidekickThemeActive.current
    val hostScheme = MaterialTheme.colorScheme
    val overlayScheme = when {
        sidekickThemeActive -> hostScheme
        !hostScheme.isM3Default() -> hostScheme
        else -> SidekickDefaultColorScheme
    }

    SidekickTheme(colorScheme = overlayScheme, sidekickColors = sidekickColors) {
        SidekickMenu(state, appInfo, onClose)
    }
}

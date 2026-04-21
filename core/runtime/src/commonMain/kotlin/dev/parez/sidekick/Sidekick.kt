package dev.parez.sidekick

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import dev.parez.sidekick.plugin.SidekickAppInfo
import dev.parez.sidekick.plugin.SidekickPlugin
import dev.parez.sidekick.plugin.rememberSidekickAppInfo
import dev.parez.sidekick.ui.SidekickMenu
import dev.parez.sidekick.ui.theme.SidekickTheme

/**
 * Sidekick debug menu composable.
 *
 * Renders the full-screen debug panel with plugin list / plugin detail navigation.
 * The **client** is responsible for visibility (e.g. FAB + `AnimatedVisibility`); this
 * composable only renders the menu content itself.
 *
 * ### Theme behaviour
 * - **`useSidekickTheme = true`** (default) → Sidekick applies its own Material 3 color scheme
 *   (light/dark based on system setting).
 * - **`useSidekickTheme = false`** → the host app's ambient [MaterialTheme] is inherited as-is.
 *
 * @param plugins            Plugins to show in the debug panel.
 * @param onClose            Called when the user taps the close button.
 * @param appInfo            Optional host-app metadata shown in the panel header.
 * @param state              Optional state; defaults to [rememberSidekickState].
 * @param useSidekickTheme   When true, apply the library's color scheme; when false,
 *                           inherit the host's ambient MaterialTheme.
 */
@Composable
fun Sidekick(
    plugins: List<SidekickPlugin>,
    appInfo: SidekickAppInfo? = rememberSidekickAppInfo(),
    state: SidekickState = rememberSidekickState(plugins),
    useSidekickTheme: Boolean = true,
    title: String = "Sidekick",
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    SidekickTheme(useSidekickTheme = useSidekickTheme) {
        SidekickMenu(state, appInfo, title, navigationIcon, actions)
    }
}

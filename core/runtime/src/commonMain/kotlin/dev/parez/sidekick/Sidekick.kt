package dev.parez.sidekick

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
 * The plugins list is stabilized internally by its plugin ids, so a `listOf(...)`
 * literal at the call site does not reset navigation state on recomposition.
 * Plugin instances themselves should still be wrapped in `remember { ... }` by
 * the caller, since they are stateful (in-memory stores, coroutine scopes, etc.).
 *
 * ### Theme behaviour
 * - **`useSidekickTheme = true`** (default) → Sidekick applies its own Material 3 color scheme
 *   (light/dark based on system setting). Host typography and shapes are preserved.
 * - **`useSidekickTheme = false`** → the host app's ambient [androidx.compose.material3.MaterialTheme] is inherited as-is.
 *
 * @param plugins            Plugins to show in the debug panel.
 * @param appInfo            Optional host-app metadata shown in the panel header.
 * @param useSidekickTheme   When true, apply the library's color scheme; when false,
 *                           inherit the host's ambient MaterialTheme.
 * @param title              Title shown in the plugin-list app bar.
 * @param navigationIcon     Optional leading slot on the plugin-list app bar.
 * @param actions            Optional trailing slot on the plugin-list app bar — a host
 *                           typically wires a close button here.
 * @param initialPluginId    Optional plugin id to open directly on first composition,
 *                           skipping the plugin grid. Ignored if no plugin in [plugins]
 *                           matches the id.
 */
@Composable
fun Sidekick(
    plugins: List<SidekickPlugin>,
    appInfo: SidekickAppInfo? = rememberSidekickAppInfo(),
    useSidekickTheme: Boolean = true,
    title: String = "Sidekick",
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    initialPluginId: String? = null,
) {
    val stablePlugins = remember(plugins.map { it.id }) { plugins }
    val state = rememberSidekickState(stablePlugins, initialPluginId)
    SidekickTheme(useSidekickTheme = useSidekickTheme) {
        SidekickMenu(state, appInfo, title, navigationIcon, actions)
    }
}

package dev.parez.sidekick.plugin

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A first-class card in the Sidekick debug overlay.
 *
 * Implementers expose a [Content] composable that renders when the user
 * opens the plugin from the plugin grid. The grid card itself is rendered
 * by Sidekick using [title] and [icon].
 *
 * ### Identity
 * [id] must be **stable across instances** and **unique across plugins** in
 * a single Sidekick host. Sidekick uses it both as a list key (recomposition
 * stability) and as the navigation key in [dev.parez.sidekick.SidekickState].
 * Convention: `"<vendor>.<feature>"` (e.g. `"sidekick.network-monitor"`,
 * `"com.acme.experiments"`). The reserved prefix `sidekick.` is used by
 * built-in plugins.
 *
 * ### Statefulness
 * A plugin instance typically owns long-lived state (in-memory stores, Koin
 * contexts, coroutine scopes). The host MUST `remember { … }` each instance
 * so it survives recomposition. The plugin list passed to
 * [dev.parez.sidekick.Sidekick] is stabilized internally by id, so the
 * `listOf(...)` literal itself doesn't need to be remembered.
 *
 * ### Lifecycle
 * If the plugin needs to react to the user opening/closing its detail
 * screen (e.g. start/stop a sampler), also implement [SidekickLifecycleAware].
 * Sidekick invokes `onAttach` / `onDetach` from a `DisposableEffect` keyed
 * on the plugin instance.
 *
 * ### Back navigation
 * Inside [Content], read [LocalSidekickBackNavigator] to obtain a `() -> Unit`
 * that returns to the plugin list — typically wired to a `TopAppBar` nav icon.
 */
interface SidekickPlugin {
    /** Stable, host-unique identifier. See class KDoc. */
    val id: String
    /** Display name shown on the plugin grid card and (typically) in the detail screen's app bar. */
    val title: String
    /** Icon shown on the plugin grid card. From `androidx.compose.material.icons.*`. */
    val icon: ImageVector
    /** Renders the plugin detail screen. Called when the user taps this plugin's card. */
    @Composable fun Content()
}

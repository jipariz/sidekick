package dev.parez.sidekick.plugin

/**
 * Optional extension for [SidekickPlugin] implementations that need to react to
 * composition lifecycle events.
 *
 * The Sidekick runtime calls [onAttach] when the plugin screen enters composition
 * and [onDetach] when it leaves. Both callbacks run on the main thread.
 *
 * Most plugins do not need this — Koin-managed scopes and Compose `remember` state
 * handle the common cases automatically. Implement this interface only when a plugin
 * holds resources that must be explicitly released, such as:
 * - OS-level listeners (BroadcastReceiver, ContentObserver)
 * - Polling loops that should pause when the screen is not visible
 * - File watchers or WebSocket connections tied to the plugin UI
 *
 * ```kotlin
 * class MyPlugin : SidekickPlugin, SidekickLifecycleAware {
 *     override val id = "my-plugin"
 *     override val title = "My Plugin"
 *     override val icon = Icons.Default.Build
 *
 *     override fun onAttach() { /* start polling */ }
 *     override fun onDetach() { /* stop polling */ }
 *
 *     @Composable override fun Content() { ... }
 * }
 * ```
 */
interface SidekickLifecycleAware {
    fun onAttach() {}
    fun onDetach() {}
}

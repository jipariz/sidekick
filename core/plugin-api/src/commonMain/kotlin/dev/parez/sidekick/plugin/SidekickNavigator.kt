package dev.parez.sidekick.plugin

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Cross-plugin navigation interface provided by SidekickShell.
 *
 * Plugins can use this to navigate to another plugin, optionally passing
 * a deep-link string that the target plugin interprets (e.g. a specific
 * item ID to open in its detail view).
 */
interface SidekickNavigator {
    /**
     * Navigate to the plugin with the given [pluginId].
     *
     * @param pluginId  The target plugin's [SidekickPlugin.id].
     * @param deepLink  Optional opaque string the target plugin uses to
     *                  navigate to a specific item (e.g. a network call ID).
     */
    fun navigateToPlugin(pluginId: String, deepLink: String? = null)

    /**
     * Consume and return the pending deep-link string, if any.
     * Returns null if no deep link is pending. After consumption the
     * deep link is cleared so it won't be processed again.
     */
    fun consumeDeepLink(): String?
}

/**
 * CompositionLocal carrying the [SidekickNavigator]. Available inside any
 * plugin's [SidekickPlugin.Content] composable when hosted in [SidekickShell].
 *
 * Returns a no-op navigator if accessed outside a SidekickShell context.
 */
val LocalSidekickNavigator = staticCompositionLocalOf<SidekickNavigator> {
    object : SidekickNavigator {
        override fun navigateToPlugin(pluginId: String, deepLink: String?) {}
        override fun consumeDeepLink(): String? = null
    }
}

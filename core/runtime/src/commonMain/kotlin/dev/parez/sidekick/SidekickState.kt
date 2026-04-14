package dev.parez.sidekick

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.runtime.NavKey
import dev.parez.sidekick.plugin.SidekickNavigator
import dev.parez.sidekick.plugin.SidekickPlugin
import kotlinx.serialization.Serializable

// ── Navigation keys ──────────────────────────────────────────────────────────

@Serializable
internal data object PluginListKey : NavKey

@Serializable
internal data class PluginScreenKey(val pluginId: String) : NavKey

// ── State ────────────────────────────────────────────────────────────────────

class SidekickState(val plugins: List<SidekickPlugin>) : SidekickNavigator {
    var isOpen: Boolean by mutableStateOf(false)
        internal set

    internal val backStack: SnapshotStateList<NavKey> =
        mutableListOf<NavKey>(PluginListKey).toMutableStateList()

    /**
     * Pending deep-link string set by [navigateToPlugin].
     * The target plugin reads and consumes this value.
     */
    var pendingDeepLink: String? by mutableStateOf(null)

    /** The currently active plugin, derived from the back stack. */
    val activePlugin: SidekickPlugin?
        get() {
            val current = backStack.lastOrNull()
            return if (current is PluginScreenKey) {
                plugins.firstOrNull { it.id == current.pluginId }
            } else null
        }

    internal fun open() { isOpen = true }

    internal fun close() {
        isOpen = false
        pendingDeepLink = null
        backStack.clear()
        backStack.add(PluginListKey)
    }

    internal fun selectPlugin(p: SidekickPlugin) {
        backStack.add(PluginScreenKey(p.id))
    }

    internal fun backToList() {
        pendingDeepLink = null
        if (backStack.size > 1) {
            backStack.removeLast()
        }
    }

    /**
     * Navigate to a plugin by ID, optionally with a deep link.
     * Opens the Sidekick overlay if not already open.
     */
    override fun consumeDeepLink(): String? {
        val link = pendingDeepLink
        pendingDeepLink = null
        return link
    }

    override fun navigateToPlugin(pluginId: String, deepLink: String?) {
        val plugin = plugins.firstOrNull { it.id == pluginId } ?: return
        pendingDeepLink = deepLink
        // Reset back stack to list + target plugin
        backStack.clear()
        backStack.add(PluginListKey)
        backStack.add(PluginScreenKey(plugin.id))
        if (!isOpen) isOpen = true
    }
}

@Composable
fun rememberSidekickState(plugins: List<SidekickPlugin>): SidekickState =
    remember(plugins) { SidekickState(plugins) }

package dev.parez.sidekick

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.runtime.NavKey
import dev.parez.sidekick.plugin.SidekickPlugin
import kotlinx.serialization.Serializable

// ── Navigation keys ──────────────────────────────────────────────────────────

@Serializable
internal data object PluginListKey : NavKey

@Serializable
internal data class PluginScreenKey(val pluginId: String) : NavKey

// ── State ────────────────────────────────────────────────────────────────────

class SidekickState(val plugins: List<SidekickPlugin>) {
    var isOpen: Boolean by mutableStateOf(false)
        internal set

    internal val backStack: SnapshotStateList<NavKey> =
        mutableListOf<NavKey>(PluginListKey).toMutableStateList()

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
        backStack.clear()
        backStack.add(PluginListKey)
    }

    internal fun selectPlugin(p: SidekickPlugin) {
        backStack.add(PluginScreenKey(p.id))
    }

    internal fun backToList() {
        if (backStack.size > 1) {
            backStack.removeLast()
        }
    }
}

@Composable
fun rememberSidekickState(plugins: List<SidekickPlugin>): SidekickState =
    remember(plugins) { SidekickState(plugins) }

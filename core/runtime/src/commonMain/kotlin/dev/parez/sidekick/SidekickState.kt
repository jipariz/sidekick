package dev.parez.sidekick

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.parez.sidekick.plugin.SidekickPlugin

// ── State ────────────────────────────────────────────────────────────────────

@Stable
class SidekickState(val plugins: List<SidekickPlugin>) {
    internal var selectedPluginId: String? by mutableStateOf(null)

    /** The currently active plugin, derived from the selected plugin ID. */
    val activePlugin: SidekickPlugin?
        get() = selectedPluginId?.let { id -> plugins.firstOrNull { it.id == id } }

    internal fun selectPlugin(p: SidekickPlugin) {
        selectedPluginId = p.id
    }

    internal fun backToList() {
        selectedPluginId = null
    }

    /** Resets internal navigation state. Called when the menu is dismissed. */
    fun reset() {
        selectedPluginId = null
    }
}

/**
 * Creates and remembers a [SidekickState] for the given [plugins].
 *
 * **Important:** [plugins] must be a stable list — i.e. the same instance across
 * recompositions. Passing a new `listOf(...)` literal on every recomposition will
 * cause [SidekickState] (and its navigation state) to be recreated on every frame.
 * Wrap the list in [androidx.compose.runtime.remember] at the call site:
 * ```kotlin
 * val plugins = remember { listOf(networkPlugin, logPlugin) }
 * val state = rememberSidekickState(plugins)
 * ```
 */
@Composable
fun rememberSidekickState(plugins: List<SidekickPlugin>): SidekickState =
    remember(plugins) { SidekickState(plugins) }

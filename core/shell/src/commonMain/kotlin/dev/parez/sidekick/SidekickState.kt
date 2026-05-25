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

    init {
        val duplicates = plugins.groupingBy { it.id }.eachCount().filter { it.value > 1 }.keys
        require(duplicates.isEmpty()) {
            "Duplicate Sidekick plugin id(s): ${duplicates.joinToString()}. " +
                "Each SidekickPlugin must have a unique `id` — two @SidekickPreferences " +
                "classes with the same `title` will collide; give one of them a distinct title."
        }
    }

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
 * Note: when called from [Sidekick], the plugins list is already stabilized by its plugin ids — a
 * `listOf(...)` literal at the `Sidekick(plugins = …)` call site is fine. If you call this function
 * directly (e.g. for testing), pass a list reference that is itself stable across recompositions.
 */
@Composable
fun rememberSidekickState(plugins: List<SidekickPlugin>): SidekickState =
    remember(plugins) { SidekickState(plugins) }

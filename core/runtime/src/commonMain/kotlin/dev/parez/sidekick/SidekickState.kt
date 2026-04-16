package dev.parez.sidekick

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.parez.sidekick.plugin.SidekickPlugin

// ── State ────────────────────────────────────────────────────────────────────

class SidekickState(val plugins: List<SidekickPlugin>) {
    var isOpen: Boolean by mutableStateOf(false)
        internal set

    internal var selectedPluginId: String? by mutableStateOf(null)

    /** The currently active plugin, derived from the selected plugin ID. */
    val activePlugin: SidekickPlugin?
        get() = selectedPluginId?.let { id -> plugins.firstOrNull { it.id == id } }

    internal fun open() { isOpen = true }

    internal fun close() {
        isOpen = false
        selectedPluginId = null
    }

    internal fun selectPlugin(p: SidekickPlugin) {
        selectedPluginId = p.id
    }

    internal fun backToList() {
        selectedPluginId = null
    }
}

@Composable
fun rememberSidekickState(plugins: List<SidekickPlugin>): SidekickState =
    remember(plugins) { SidekickState(plugins) }

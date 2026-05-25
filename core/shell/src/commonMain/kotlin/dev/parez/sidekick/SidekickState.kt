package dev.parez.sidekick

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
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

    /**
     * Mutable, user-driven plugin order. Initially mirrors [plugins]; the menu UI mutates it via
     * [movePlugin] when the user drags a tile. [Sidekick] hydrates this from persistent storage on
     * first compose and writes back on every change.
     */
    internal val orderedPluginIds: SnapshotStateList<String> =
        mutableStateListOf<String>().apply { addAll(plugins.map { it.id }) }

    /** Plugins in user-chosen order. Drives the menu grid and the public list view. */
    val orderedPlugins: List<SidekickPlugin> by derivedStateOf {
        orderedPluginIds.mapNotNull { id -> plugins.firstOrNull { it.id == id } }
    }

    /** The currently active plugin, derived from the selected plugin ID. */
    val activePlugin: SidekickPlugin?
        get() = selectedPluginId?.let { id -> plugins.firstOrNull { it.id == id } }

    internal fun selectPlugin(p: SidekickPlugin) {
        selectedPluginId = p.id
    }

    internal fun backToList() {
        selectedPluginId = null
    }

    /**
     * Reorder via plugin id keys. Looking up by key (rather than the LazyGrid item index) sidesteps
     * the sticky-header offset — the header doesn't take a slot in [orderedPluginIds], but it does
     * occupy grid index 0.
     */
    internal fun movePluginByKey(fromKey: String, toKey: String) {
        if (fromKey == toKey) return
        val fromIdx = orderedPluginIds.indexOf(fromKey)
        val toIdx = orderedPluginIds.indexOf(toKey)
        if (fromIdx == -1 || toIdx == -1) return
        orderedPluginIds.add(toIdx, orderedPluginIds.removeAt(fromIdx))
    }

    /**
     * Re-arrange [orderedPluginIds] so that ids present in [persisted] appear first in
     * [persisted]'s order, followed by any new-since-persist ids in their input order. Ids in
     * [persisted] that don't correspond to any installed plugin are ignored. No-op when [persisted]
     * is empty.
     */
    internal fun applyPersistedOrder(persisted: List<String>) {
        if (persisted.isEmpty()) return
        val installed = plugins.mapTo(LinkedHashSet()) { it.id }
        val seen = HashSet<String>(installed.size)
        val newOrder = ArrayList<String>(installed.size)
        for (id in persisted) {
            if (id in installed && seen.add(id)) newOrder.add(id)
        }
        for (id in installed) {
            if (seen.add(id)) newOrder.add(id)
        }
        if (newOrder != orderedPluginIds.toList()) {
            orderedPluginIds.clear()
            orderedPluginIds.addAll(newOrder)
        }
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

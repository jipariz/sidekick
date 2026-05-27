package dev.parez.sidekick.network.ui.persistence

import java.util.prefs.Preferences

private object JvmPaneSizeStore : PaneSizeStore {
    private val node: Preferences =
        Preferences.userRoot().node("dev/parez/sidekick/network-monitor/pane-sizes")

    override suspend fun read(): NetworkMonitorPaneSizes? =
        decodePaneSizes(node.get(PANE_SIZE_KEY, null))

    override suspend fun write(sizes: NetworkMonitorPaneSizes) {
        node.put(PANE_SIZE_KEY, encodePaneSizes(sizes))
        node.flush()
    }
}

internal actual fun createPaneSizeStore(): PaneSizeStore = JvmPaneSizeStore

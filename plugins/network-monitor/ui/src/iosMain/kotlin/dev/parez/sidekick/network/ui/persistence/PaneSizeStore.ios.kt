package dev.parez.sidekick.network.ui.persistence

import platform.Foundation.NSUserDefaults

private object IosPaneSizeStore : PaneSizeStore {
    private val defaults: NSUserDefaults
        get() = NSUserDefaults.standardUserDefaults

    override suspend fun read(): NetworkMonitorPaneSizes? =
        decodePaneSizes(defaults.stringForKey(PANE_SIZE_KEY))

    override suspend fun write(sizes: NetworkMonitorPaneSizes) {
        defaults.setObject(encodePaneSizes(sizes), PANE_SIZE_KEY)
    }
}

internal actual fun createPaneSizeStore(): PaneSizeStore = IosPaneSizeStore

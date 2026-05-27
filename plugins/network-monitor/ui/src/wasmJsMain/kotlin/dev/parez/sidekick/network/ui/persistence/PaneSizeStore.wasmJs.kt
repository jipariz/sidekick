package dev.parez.sidekick.network.ui.persistence

import kotlinx.browser.localStorage
import org.w3c.dom.get
import org.w3c.dom.set

private object WasmJsPaneSizeStore : PaneSizeStore {
    override suspend fun read(): NetworkMonitorPaneSizes? =
        decodePaneSizes(localStorage[PANE_SIZE_KEY])

    override suspend fun write(sizes: NetworkMonitorPaneSizes) {
        localStorage[PANE_SIZE_KEY] = encodePaneSizes(sizes)
    }
}

internal actual fun createPaneSizeStore(): PaneSizeStore = WasmJsPaneSizeStore

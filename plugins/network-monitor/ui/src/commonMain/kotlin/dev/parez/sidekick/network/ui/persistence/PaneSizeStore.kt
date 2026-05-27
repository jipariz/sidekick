package dev.parez.sidekick.network.ui.persistence

internal data class NetworkMonitorPaneSizes(
    val listAnchorIndex: Int,
    val requestResponseProportion: Float,
)

internal interface PaneSizeStore {
    suspend fun read(): NetworkMonitorPaneSizes?

    suspend fun write(sizes: NetworkMonitorPaneSizes)
}

internal expect fun createPaneSizeStore(): PaneSizeStore

internal const val PANE_SIZE_STORE_NAME = "sidekick_network_monitor_pane_sizes"
internal const val PANE_SIZE_KEY = "sidekick.network_monitor.pane_sizes"

internal fun encodePaneSizes(sizes: NetworkMonitorPaneSizes): String =
    "${sizes.listAnchorIndex},${sizes.requestResponseProportion}"

internal fun decodePaneSizes(raw: String?): NetworkMonitorPaneSizes? {
    if (raw.isNullOrEmpty()) return null
    val parts = raw.split(',')
    if (parts.size != 2) return null
    val index = parts[0].toIntOrNull() ?: return null
    val proportion = parts[1].toFloatOrNull() ?: return null
    return NetworkMonitorPaneSizes(index, proportion)
}

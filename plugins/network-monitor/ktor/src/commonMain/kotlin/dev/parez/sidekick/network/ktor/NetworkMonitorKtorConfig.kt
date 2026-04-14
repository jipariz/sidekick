package dev.parez.sidekick.network.ktor

import dev.parez.sidekick.logs.LogMonitorStore
import dev.parez.sidekick.network.NetworkMonitorStore
import dev.parez.sidekick.network.RetentionPeriod
import io.ktor.client.request.HttpRequestBuilder

public class NetworkMonitorKtorConfig {
    internal val filters = mutableListOf<(HttpRequestBuilder) -> Boolean>()
    internal val sanitizedHeaders = mutableListOf<SanitizedHeader>()

    /** Maximum number of characters captured from a request/response body. */
    public var maxContentLength: Int = 65_536

    /**
     * Calls older than this many milliseconds are purged on next [NetworkMonitorStore.init].
     * Use constants from [RetentionPeriod].
     */
    public var retentionPeriod: Long = RetentionPeriod.ONE_HOUR

    /** The store to write captured calls into. Override for testing. */
    public var store: NetworkMonitorStore = NetworkMonitorStore

    /**
     * Optional log store for automatic log-network correlation.
     * When set, the interceptor emits log entries for each HTTP request and
     * response with `networkCallId` metadata, enabling the log-monitor plugin
     * to link log entries to network calls.
     *
     * Set to [LogMonitorStore] to enable:
     * ```
     * install(NetworkMonitorKtor) {
     *     logStore = LogMonitorStore
     * }
     * ```
     *
     * When null (default), no log entries are emitted.
     */
    public var logStore: LogMonitorStore? = null

    /**
     * Exclude requests matching [predicate] from being recorded.
     * If no filters are registered, all requests are recorded.
     */
    public fun filter(predicate: (HttpRequestBuilder) -> Boolean) {
        filters += predicate
    }

    /**
     * Replace the value of any header whose name matches [predicate] with [placeholder].
     * Call multiple times to sanitize multiple headers.
     */
    public fun sanitizeHeader(placeholder: String = "***", predicate: (String) -> Boolean) {
        sanitizedHeaders += SanitizedHeader(placeholder, predicate)
    }

    internal fun shouldLog(request: HttpRequestBuilder): Boolean =
        filters.isEmpty() || filters.none { it(request) }
}

internal data class SanitizedHeader(val placeholder: String, val predicate: (String) -> Boolean)

package dev.parez.sidekick.network.ktor

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
     * Optional callback invoked for each captured network request.
     * Receives the call ID, HTTP method, and URL. Use this to emit
     * correlated log entries (e.g. via Kermit with a networkCallId metadata tag).
     */
    public var onRequest: ((id: String, method: String, url: String) -> Unit)? = null

    /**
     * Optional callback invoked when a network response is received.
     * Receives the call ID, HTTP status code, and URL.
     */
    public var onResponse: ((id: String, statusCode: Int, url: String) -> Unit)? = null

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

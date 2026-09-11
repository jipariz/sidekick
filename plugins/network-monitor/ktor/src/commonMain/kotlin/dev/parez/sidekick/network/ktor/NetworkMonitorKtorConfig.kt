package dev.parez.sidekick.network.ktor

import dev.parez.sidekick.network.NetworkMonitorStore
import dev.parez.sidekick.network.di.NetworkMonitorKoinContext
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.utils.io.KtorDsl
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * The maximum length of the content that will be logged. After this, request/response body will be
 * truncated.
 */
public object ContentLength {
    /** Default value: 65,536 characters. */
    public const val Default: Int = 65_536

    /** Log the full content without truncation. */
    public const val Full: Int = Int.MAX_VALUE
}

/**
 * Header names whose values Sidekick replaces with a placeholder before recording, unless
 * [NetworkMonitorKtorConfig.disableDefaultRedaction] is called. Matched case-insensitively.
 *
 * Captured calls are persisted to a Room database that survives app restarts, so credentials that
 * reach the monitor outlive the process that sent them. These are redacted by default for that
 * reason.
 */
public val DefaultRedactedHeaders: Set<String> =
    setOf("authorization", "proxy-authorization", "cookie", "set-cookie", "x-api-key")

@KtorDsl
public class NetworkMonitorKtorConfig {
    internal val filters = mutableListOf<(HttpRequestBuilder) -> Boolean>()
    internal val sanitizedHeaders = mutableListOf<SanitizedHeader>()

    private var defaultRedactionEnabled = true

    /** Maximum number of characters captured from a request/response body. */
    public var maxContentLength: Int = ContentLength.Default

    /** Calls older than this duration are purged on next [NetworkMonitorStore.init]. */
    public var retentionPeriod: Duration = 1.hours

    /** The store to write captured calls into. Override for testing. */
    public var store: NetworkMonitorStore = NetworkMonitorKoinContext.getDefaultStore()

    /**
     * Exclude requests matching [predicate] from being recorded. If no filters are registered, all
     * requests are recorded.
     */
    public fun filter(predicate: (HttpRequestBuilder) -> Boolean) {
        filters += predicate
    }

    /**
     * Replace the value of any header whose name matches [predicate] with [placeholder]. Call
     * multiple times to sanitize multiple headers.
     *
     * Sanitizers registered here take precedence over [DefaultRedactedHeaders], so passing a
     * predicate that matches a default-redacted name replaces the built-in placeholder rather than
     * being shadowed by it.
     */
    public fun sanitizeHeader(placeholder: String = "***", predicate: (String) -> Boolean) {
        sanitizedHeaders += SanitizedHeader(placeholder, predicate)
    }

    /**
     * Stops redacting [DefaultRedactedHeaders]. Call only when capturing credential headers is
     * intended — recorded calls are written to disk and survive app restarts.
     */
    public fun disableDefaultRedaction() {
        defaultRedactionEnabled = false
    }

    internal fun shouldLog(request: HttpRequestBuilder): Boolean =
        filters.isEmpty() || filters.none { it(request) }

    /**
     * Consumer-registered sanitizers first so they can override a built-in entry, then the default
     * redaction set. Read by the Ktor plugin at capture time.
     */
    internal val effectiveSanitizedHeaders: List<SanitizedHeader>
        get() =
            if (defaultRedactionEnabled) sanitizedHeaders + DefaultRedaction else sanitizedHeaders

    internal companion object {
        private val DefaultRedaction =
            SanitizedHeader(placeholder = "***") { it.lowercase() in DefaultRedactedHeaders }
    }
}

internal data class SanitizedHeader(val placeholder: String, val predicate: (String) -> Boolean)

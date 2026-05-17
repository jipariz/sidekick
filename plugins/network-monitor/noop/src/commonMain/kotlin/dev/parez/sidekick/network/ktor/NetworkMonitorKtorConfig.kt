package dev.parez.sidekick.network.ktor

import dev.parez.sidekick.network.NetworkMonitorStore
import dev.parez.sidekick.network.di.NetworkMonitorKoinContext
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.utils.io.KtorDsl
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Release-variant stub for `ContentLength` — values kept identical so call sites
 * referencing `ContentLength.Default` / `ContentLength.Full` still resolve.
 */
public object ContentLength {
    public const val Default: Int = 65_536
    public const val Full: Int = Int.MAX_VALUE
}

/**
 * Release-variant stub for `NetworkMonitorKtorConfig`. Accepts every configuration
 * call from the real DSL but discards it.
 */
@KtorDsl
@Suppress("UNUSED_PARAMETER")
public class NetworkMonitorKtorConfig {
    public var maxContentLength: Int = ContentLength.Default
    public var retentionPeriod: Duration = 1.hours
    public var store: NetworkMonitorStore = NetworkMonitorKoinContext.getDefaultStore()

    public fun filter(predicate: (HttpRequestBuilder) -> Boolean) = Unit

    public fun sanitizeHeader(placeholder: String = "***", predicate: (String) -> Boolean) = Unit
}

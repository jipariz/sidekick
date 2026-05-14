package dev.parez.sidekick.network.ktor

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/**
 * Installs the Sidekick network-monitor Ktor plugin onto this [HttpClientConfig].
 *
 * Equivalent to `install(NetworkMonitorKtor) { configure() }` but reads as
 * "this client reports to Sidekick" at the call site.
 *
 * ```kotlin
 * val client = HttpClient {
 *     install(ContentNegotiation) { json() }
 *     installSidekick()                   // <-- one line
 * }
 * ```
 *
 * @param configure Optional [NetworkMonitorKtorConfig] customization (custom store,
 *                  sanitized headers, body-size limits, request filter).
 */
fun HttpClientConfig<*>.installSidekick(
    configure: NetworkMonitorKtorConfig.() -> Unit = {},
) {
    install(NetworkMonitorKtor, configure)
}

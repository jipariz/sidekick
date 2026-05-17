package dev.parez.sidekick.network.ktor

import io.ktor.client.HttpClientConfig

/**
 * Release-variant stub for `installSidekick`. Installs the noop
 * [NetworkMonitorKtor] plugin so the consumer call site stays identical, but no
 * recording hooks are registered.
 */
fun HttpClientConfig<*>.installSidekick(
    configure: NetworkMonitorKtorConfig.() -> Unit = {},
) {
    install(NetworkMonitorKtor, configure)
}

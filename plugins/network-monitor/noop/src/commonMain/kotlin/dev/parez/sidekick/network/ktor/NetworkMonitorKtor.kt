package dev.parez.sidekick.network.ktor

import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin

/**
 * Release-variant stub for `NetworkMonitorKtor`. Registers no `on(...)` hooks, so installing it
 * onto an `HttpClient` is a true no-op — requests pass through untouched and nothing is recorded.
 */
public val NetworkMonitorKtor: ClientPlugin<NetworkMonitorKtorConfig> =
    createClientPlugin("NetworkMonitorKtor", ::NetworkMonitorKtorConfig) {}

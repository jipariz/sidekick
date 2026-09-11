package dev.parez.sidekick.network.di

import dev.parez.sidekick.network.NetworkMonitorStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinApplication

/**
 * Release-variant stub for `NetworkMonitorKoinContext`. Exposes the same public symbols as the real
 * plugin so consumer code compiles unchanged, but holds an empty Koin graph and returns a no-op
 * store.
 */
public object NetworkMonitorKoinContext {

    public val koinApp: KoinApplication = koinApplication {}

    private val defaultStore: NetworkMonitorStore = NetworkMonitorStore()

    public fun getDefaultStore(): NetworkMonitorStore = defaultStore

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    public fun storeScope(): CoroutineScope = scope

    @Suppress("UNUSED_PARAMETER") public fun loadViewModelModule(module: Module) = Unit
}

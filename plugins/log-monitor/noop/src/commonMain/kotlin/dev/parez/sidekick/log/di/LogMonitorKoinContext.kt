package dev.parez.sidekick.log.di

import dev.parez.sidekick.log.LogMonitorStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinApplication

/**
 * Release-variant stub for `LogMonitorKoinContext`. Exposes the same public symbols as the real
 * plugin so consumer code compiles unchanged, but holds an empty Koin graph and returns the no-op
 * `LogMonitorStore` singleton.
 */
public object LogMonitorKoinContext {

    public val koinApp: KoinApplication = koinApplication {}

    public fun getDefaultStore(): LogMonitorStore = LogMonitorStore

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    public fun storeScope(): CoroutineScope = scope

    @Suppress("UNUSED_PARAMETER") public fun loadViewModelModule(module: Module): Unit = Unit
}

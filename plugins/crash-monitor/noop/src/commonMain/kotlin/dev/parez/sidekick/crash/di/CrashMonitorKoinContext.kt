package dev.parez.sidekick.crash.di

import dev.parez.sidekick.crash.CrashMonitorStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinApplication

/** Release-variant stub for `CrashMonitorKoinContext` — empty graph, no-op store. */
public object CrashMonitorKoinContext {

    public val koinApp: KoinApplication = koinApplication {}

    private val defaultStore = CrashMonitorStore()

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    public fun getDefaultStore(): CrashMonitorStore = defaultStore

    public fun storeScope(): CoroutineScope = scope

    @Suppress("UNUSED_PARAMETER") public fun loadViewModelModule(module: Module): Unit = Unit
}

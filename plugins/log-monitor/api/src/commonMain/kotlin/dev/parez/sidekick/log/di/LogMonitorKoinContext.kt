package dev.parez.sidekick.log.di

import dev.parez.sidekick.log.LogMonitorStore
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * Isolated Koin context for the Log Monitor plugin.
 *
 * Holds its own [KoinApplication] so the plugin's DI graph never leaks into or conflicts with a
 * consuming application's Koin instance.
 *
 * The [logMonitorCoreModule] registers [LogMonitorStore] as a singleton. The `log-monitor:plugin`
 * module extends this context with its ViewModel module via [loadViewModelModule], called once from
 * [dev.parez.sidekick.log.LogMonitorPlugin].
 */
public object LogMonitorKoinContext {

    public val koinApp: KoinApplication = koinApplication { modules(logMonitorCoreModule) }

    internal val koin
        get() = koinApp.koin

    private val viewModelModuleLoaded = MutableStateFlow(false)

    public fun getDefaultStore(): LogMonitorStore = koin.get()

    public fun loadViewModelModule(module: Module) {
        if (viewModelModuleLoaded.compareAndSet(expect = false, update = true)) {
            koinApp.koin.loadModules(listOf(module))
        }
    }
}

internal val logMonitorCoreModule = module { single { LogMonitorStore } }

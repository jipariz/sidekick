package dev.parez.sidekick.crash.di

import dev.parez.sidekick.crash.CrashMonitorStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/** Isolated Koin context for the Crash Monitor plugin. */
public object CrashMonitorKoinContext {

    public val koinApp: KoinApplication = koinApplication { modules(crashMonitorCoreModule) }

    internal val koin
        get() = koinApp.koin

    private val viewModelModuleLoaded = MutableStateFlow(false)

    public fun getDefaultStore(): CrashMonitorStore = koin.get()

    public fun storeScope(): CoroutineScope = koin.get()

    public fun loadViewModelModule(module: Module) {
        if (viewModelModuleLoaded.compareAndSet(expect = false, update = true)) {
            koinApp.koin.loadModules(listOf(module))
        }
    }
}

internal val crashMonitorCoreModule = module {
    single { CoroutineScope(Dispatchers.Default + SupervisorJob()) }
    single { CrashMonitorStore() }
}

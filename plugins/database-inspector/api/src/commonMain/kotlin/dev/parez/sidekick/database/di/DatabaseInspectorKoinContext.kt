package dev.parez.sidekick.database.di

import dev.parez.sidekick.database.DatabaseInspectorStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * Isolated Koin context for the Database Inspector plugin, mirroring the network and log monitors:
 * the plugin's graph never touches the consuming application's Koin instance.
 */
public object DatabaseInspectorKoinContext {

    public val koinApp: KoinApplication = koinApplication { modules(databaseInspectorCoreModule) }

    internal val koin
        get() = koinApp.koin

    private val viewModelModuleLoaded = MutableStateFlow(false)

    /**
     * The singleton store. `:room` reaches for this so it needs no Koin dependency of its own —
     * same arrangement as `network-monitor:ktor`.
     */
    public fun getDefaultStore(): DatabaseInspectorStore = koin.get()

    public fun storeScope(): CoroutineScope = koin.get()

    public fun loadViewModelModule(module: Module) {
        if (viewModelModuleLoaded.compareAndSet(expect = false, update = true)) {
            koinApp.koin.loadModules(listOf(module))
        }
    }
}

internal val databaseInspectorCoreModule = module {
    single { CoroutineScope(Dispatchers.Default + SupervisorJob()) }
    single { DatabaseInspectorStore() }
}

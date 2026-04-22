package dev.parez.sidekick.network.di

import dev.parez.sidekick.network.NetworkMonitorStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * Isolated Koin context for the Network Monitor plugin.
 *
 * Holds its own [KoinApplication] so the plugin's DI graph never leaks into
 * or conflicts with a consuming application's Koin instance.
 *
 * The [networkMonitorCoreModule] registers:
 * - A shared [CoroutineScope] for background I/O
 * - [NetworkMonitorStore] as a singleton (the single source of truth for HTTP traffic)
 *
 * The `network-monitor:plugin` module extends this context with its ViewModel module
 * via [loadViewModelModule], called once from [dev.parez.sidekick.network.NetworkMonitorPlugin].
 *
 * Usage in Compose:
 * ```kotlin
 * KoinIsolatedContext(context = NetworkMonitorKoinContext.koinApp) {
 *     // composables here resolve from the plugin's isolated DI graph
 * }
 * ```
 */
public object NetworkMonitorKoinContext {

    public val koinApp: KoinApplication = koinApplication {
        modules(networkMonitorCoreModule)
    }

    public val koin get() = koinApp.koin

    @Volatile
    private var viewModelModuleLoaded = false
    private val lock = Any()

    /**
     * Returns the singleton [NetworkMonitorStore] from this isolated Koin context.
     * Used as the default value in [dev.parez.sidekick.network.ktor.NetworkMonitorKtorConfig]
     * so that the `network-monitor:ktor` module does not need a direct Koin dependency.
     */
    public fun getDefaultStore(): NetworkMonitorStore = koin.get()

    /**
     * Loads an additional [module] (e.g. the ViewModel module from `network-monitor:plugin`)
     * into this context exactly once. Subsequent calls are no-ops.
     */
    public fun loadViewModelModule(module: Module) {
        if (!viewModelModuleLoaded) {
            synchronized(lock) {
                if (!viewModelModuleLoaded) {
                    viewModelModuleLoaded = true
                    koinApp.koin.loadModules(listOf(module))
                }
            }
        }
    }
}

internal val networkMonitorCoreModule = module {
    single { CoroutineScope(Dispatchers.Default + SupervisorJob()) }
    singleOf(::NetworkMonitorStore)
}
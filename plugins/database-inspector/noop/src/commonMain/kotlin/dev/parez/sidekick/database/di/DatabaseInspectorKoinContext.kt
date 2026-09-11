package dev.parez.sidekick.database.di

import dev.parez.sidekick.database.DatabaseInspectorStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinApplication

/** Release-variant stub for `DatabaseInspectorKoinContext` — empty graph, no-op store. */
public object DatabaseInspectorKoinContext {

    public val koinApp: KoinApplication = koinApplication {}

    private val defaultStore = DatabaseInspectorStore()

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    public fun getDefaultStore(): DatabaseInspectorStore = defaultStore

    public fun storeScope(): CoroutineScope = scope

    @Suppress("UNUSED_PARAMETER") public fun loadViewModelModule(module: Module): Unit = Unit
}

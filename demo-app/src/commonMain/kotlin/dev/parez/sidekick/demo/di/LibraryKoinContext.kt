package dev.parez.sidekick.demo.di

import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication

/**
 * Isolated Koin context for the demo library.
 *
 * Holds its own [KoinApplication] so the library's DI graph never leaks into
 * or conflicts with a consuming application's Koin instance.
 *
 * Usage in Compose:
 * ```kotlin
 * KoinIsolatedContext(context = LibraryKoinContext.koinApp) {
 *     // composables here resolve from the library's isolated graph
 * }
 * ```
 */
object LibraryKoinContext {
    val koinApp: KoinApplication = koinApplication {
        modules(appModule)
    }
}

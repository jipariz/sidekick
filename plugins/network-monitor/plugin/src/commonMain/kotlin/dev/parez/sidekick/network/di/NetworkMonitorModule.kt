package dev.parez.sidekick.network.di

import dev.parez.sidekick.network.NetworkMonitorViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module providing the Network Monitor ViewModel.
 *
 * Loaded into [NetworkMonitorKoinContext] exactly once via
 * [NetworkMonitorKoinContext.loadViewModelModule] when [dev.parez.sidekick.network.NetworkMonitorPlugin]
 * is first instantiated.
 */
internal val networkMonitorViewModelModule = module {
    viewModelOf(::NetworkMonitorViewModel)
}

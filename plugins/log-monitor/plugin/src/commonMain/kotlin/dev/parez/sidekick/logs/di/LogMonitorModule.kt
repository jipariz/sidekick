package dev.parez.sidekick.logs.di

import dev.parez.sidekick.logs.LogMonitorViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val logMonitorViewModelModule = module {
    viewModelOf(::LogMonitorViewModel)
}

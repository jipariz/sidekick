package dev.parez.sidekick.log.di

import dev.parez.sidekick.log.LogMonitorViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val logMonitorViewModelModule = module {
    viewModelOf(::LogMonitorViewModel)
}

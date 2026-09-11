package dev.parez.sidekick.crash.di

import dev.parez.sidekick.crash.CrashMonitorViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val crashMonitorViewModelModule = module { viewModelOf(::CrashMonitorViewModel) }

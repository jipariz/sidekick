package dev.parez.sidekick.database.di

import dev.parez.sidekick.database.DatabaseInspectorViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val databaseInspectorViewModelModule = module { viewModelOf(::DatabaseInspectorViewModel) }

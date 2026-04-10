package dev.parez.sidekick.demo.di

import dev.parez.sidekick.demo.PokemonApi
import dev.parez.sidekick.demo.PokemonRepository
import dev.parez.sidekick.demo.db.PokemonCache
import dev.parez.sidekick.demo.db.createPokemonCache
import dev.parez.sidekick.demo.ui.PokemonDetailViewModel
import dev.parez.sidekick.demo.ui.PokemonListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { PokemonApi() }
    single<PokemonCache> { createPokemonCache() }
    single { PokemonRepository(get(), get()) }
    viewModelOf(::PokemonListViewModel)
    viewModel { params -> PokemonDetailViewModel(params.get(), get()) }
}

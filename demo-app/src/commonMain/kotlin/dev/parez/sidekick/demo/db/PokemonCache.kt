package dev.parez.sidekick.demo.db

import dev.parez.sidekick.demo.PokemonDetail
import dev.parez.sidekick.demo.PokemonListEntry
import kotlinx.coroutines.flow.Flow

interface PokemonCache {
    fun observeAll(): Flow<List<PokemonListEntry>>
    fun observeDetail(id: Int): Flow<PokemonDetail?>
    suspend fun saveListEntries(entries: List<PokemonListEntry>)
    suspend fun saveDetail(detail: PokemonDetail)
}

expect fun createPokemonCache(): PokemonCache

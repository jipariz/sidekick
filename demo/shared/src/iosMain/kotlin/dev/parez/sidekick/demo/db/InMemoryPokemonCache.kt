package dev.parez.sidekick.demo.db

import dev.parez.sidekick.demo.PokemonDetail
import dev.parez.sidekick.demo.PokemonListEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory [PokemonCache] used on iOS targets, where Room 3 (`androidx.room3:room3-runtime`)
 * does not yet publish a `*-iosArm64` / `*-iosX64` / `*-iosSimulatorArm64` variant
 * (as of 3.0.0-alpha03). When Room ships iOS support, replace this with a
 * RoomPokemonCache instance from the shared `nonIosMain` source set.
 */
class InMemoryPokemonCache : PokemonCache {

    private val listFlow = MutableStateFlow<List<PokemonListEntry>>(emptyList())
    private val detailsById = MutableStateFlow<Map<Int, PokemonDetail>>(emptyMap())

    override fun observeAll(): Flow<List<PokemonListEntry>> = listFlow.asStateFlow()

    override fun observeDetail(id: Int): Flow<PokemonDetail?> =
        detailsById.map { it[id] }

    override suspend fun saveListEntries(entries: List<PokemonListEntry>) {
        listFlow.value = entries
    }

    override suspend fun saveDetail(detail: PokemonDetail) {
        detailsById.update { it + (detail.id to detail) }
    }
}

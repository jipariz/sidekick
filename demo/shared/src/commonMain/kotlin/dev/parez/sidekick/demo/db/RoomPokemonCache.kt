package dev.parez.sidekick.demo.db

import dev.parez.sidekick.demo.AbilitySlot
import dev.parez.sidekick.demo.PokemonDetail
import dev.parez.sidekick.demo.PokemonListEntry
import dev.parez.sidekick.demo.StatEntry
import dev.parez.sidekick.demo.TypeSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoomPokemonCache(private val dao: PokemonCacheDao) : PokemonCache {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override fun observeAll(): Flow<List<PokemonListEntry>> =
        dao.observeListEntries()
            .onStart { emit(emptyList()) }
            .map { rows -> rows.map { it.toListEntry() } }

    override fun observeDetail(id: Int): Flow<PokemonDetail?> =
        dao.observeDetail(id).onStart { emit(null) }.map { row -> row?.toDetail() }

    override suspend fun saveListEntries(entries: List<PokemonListEntry>) {
        dao.upsertListEntries(entries.map { it.toListRow() })
    }

    override suspend fun saveDetail(detail: PokemonDetail) {
        dao.upsertDetail(detail.toDetailRow())
    }

    // ── Mapping helpers ──────────────────────────────────────────────────────

    private fun PokemonListEntity.toListEntry() =
        PokemonListEntry(name = name, url = "https://pokeapi.co/api/v2/pokemon/$id/")

    private fun PokemonDetailEntity.toDetail() =
        PokemonDetail(
            id = id,
            name = name,
            height = height,
            weight = weight,
            types = json.decodeFromString<List<TypeSlot>>(typesJson),
            stats = json.decodeFromString<List<StatEntry>>(statsJson),
            abilities = json.decodeFromString<List<AbilitySlot>>(abilitiesJson),
        )

    private fun PokemonListEntry.toListRow() = PokemonListEntity(id = id, name = name)

    private fun PokemonDetail.toDetailRow() =
        PokemonDetailEntity(
            id = id,
            name = name,
            height = height,
            weight = weight,
            typesJson = json.encodeToString(types),
            statsJson = json.encodeToString(stats),
            abilitiesJson = json.encodeToString(abilities),
        )
}

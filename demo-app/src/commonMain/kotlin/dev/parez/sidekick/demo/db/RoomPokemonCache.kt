package dev.parez.sidekick.demo.db

import dev.parez.sidekick.demo.AbilitySlot
import dev.parez.sidekick.demo.PokemonDetail
import dev.parez.sidekick.demo.PokemonListEntry
import dev.parez.sidekick.demo.StatEntry
import dev.parez.sidekick.demo.TypeSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoomPokemonCache(private val dao: PokemonCacheDao) : PokemonCache {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    override fun observeAll(): Flow<List<PokemonListEntry>> =
        dao.observeAll()
            .onStart { emit(emptyList()) }
            .onEach { println("[RoomCache] observeAll emitted ${it.size} entities") }
            .map { entities -> entities.map { it.toListEntry() } }

    override fun observeDetail(id: Int): Flow<PokemonDetail?> =
        dao.observeById(id)
            .onStart { emit(null) }
            .onEach { println("[RoomCache] observeById($id) emitted: ${it != null}") }
            .map { entity -> entity?.toDetail() }

    override suspend fun saveListEntries(entries: List<PokemonListEntry>) {
        println("[RoomCache] saveListEntries: ${entries.size} entries")
        dao.upsertAll(entries.map { it.toEntity() })
        println("[RoomCache] saveListEntries: done")
    }

    override suspend fun saveDetail(detail: PokemonDetail) {
        println("[RoomCache] saveDetail: id=${detail.id}")
        dao.upsert(detail.toEntity())
        println("[RoomCache] saveDetail: done")
    }

    // ── Mapping helpers ──────────────────────────────────────────────────────

    private fun PokemonEntity.toListEntry() = PokemonListEntry(
        name = name,
        url = "https://pokeapi.co/api/v2/pokemon/$id/",
    )

    private fun PokemonEntity.toDetail(): PokemonDetail? {
        if (height == null || weight == null) return null
        return PokemonDetail(
            id = id,
            name = name,
            height = height,
            weight = weight,
            types = typesJson?.let { json.decodeFromString<List<TypeSlot>>(it) } ?: emptyList(),
            stats = statsJson?.let { json.decodeFromString<List<StatEntry>>(it) } ?: emptyList(),
            abilities = abilitiesJson?.let { json.decodeFromString<List<AbilitySlot>>(it) } ?: emptyList(),
        )
    }

    private fun PokemonListEntry.toEntity() = PokemonEntity(
        id = id,
        name = name,
    )

    private fun PokemonDetail.toEntity() = PokemonEntity(
        id = id,
        name = name,
        height = height,
        weight = weight,
        typesJson = json.encodeToString(types),
        statsJson = json.encodeToString(stats),
        abilitiesJson = json.encodeToString(abilities),
    )
}

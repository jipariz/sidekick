package dev.parez.sidekick.demo.db

import dev.parez.sidekick.demo.PokemonDetail
import dev.parez.sidekick.demo.PokemonListResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoomPokemonCache(private val dao: PokemonCacheDao) : PokemonCache {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    override suspend fun getListPage(offset: Int, limit: Int): PokemonListResponse? {
        val cached = dao.getListPage(offset) ?: return null
        return json.decodeFromString(cached.responseJson)
    }

    override suspend fun saveListPage(offset: Int, limit: Int, response: PokemonListResponse) {
        dao.upsertListPage(
            CachedListPageEntity(
                offset = offset,
                responseJson = json.encodeToString(response),
            )
        )
    }

    override suspend fun getDetail(id: Int): PokemonDetail? {
        val cached = dao.getDetail(id) ?: return null
        return json.decodeFromString(cached.responseJson)
    }

    override suspend fun saveDetail(detail: PokemonDetail) {
        dao.upsertDetail(
            CachedDetailEntity(
                id = detail.id,
                responseJson = json.encodeToString(detail),
            )
        )
    }
}

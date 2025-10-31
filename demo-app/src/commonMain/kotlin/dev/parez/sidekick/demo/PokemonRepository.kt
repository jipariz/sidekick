package dev.parez.sidekick.demo

import dev.parez.sidekick.demo.db.PokemonCache

class PokemonRepository(
    private val api: PokemonApi,
    private val cache: PokemonCache,
) {
    suspend fun getListPage(offset: Int, limit: Int = 20): PokemonListResponse {
        cache.getListPage(offset, limit)?.let { return it }
        val response = api.fetchList(offset, limit)
        cache.saveListPage(offset, limit, response)
        return response
    }

    suspend fun getDetail(id: Int): PokemonDetail {
        cache.getDetail(id)?.let { return it }
        val detail = api.fetchDetail(id)
        cache.saveDetail(detail)
        return detail
    }
}

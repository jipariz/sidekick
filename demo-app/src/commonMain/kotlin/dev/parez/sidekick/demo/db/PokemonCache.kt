package dev.parez.sidekick.demo.db

import dev.parez.sidekick.demo.PokemonDetail
import dev.parez.sidekick.demo.PokemonListResponse

interface PokemonCache {
    suspend fun getListPage(offset: Int, limit: Int): PokemonListResponse?
    suspend fun saveListPage(offset: Int, limit: Int, response: PokemonListResponse)
    suspend fun getDetail(id: Int): PokemonDetail?
    suspend fun saveDetail(detail: PokemonDetail)
}

expect fun createPokemonCache(): PokemonCache

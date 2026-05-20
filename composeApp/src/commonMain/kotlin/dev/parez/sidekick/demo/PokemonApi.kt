package dev.parez.sidekick.demo

import io.ktor.client.call.body
import io.ktor.client.request.get

private const val BASE = "https://pokeapi.co/api/v2"

class PokemonApi(private val client: io.ktor.client.HttpClient = pokeHttpClient) {

    suspend fun fetchList(offset: Int = 0, limit: Int = 20): PokemonListResponse =
        client.get("$BASE/pokemon?offset=$offset&limit=$limit").body()

    suspend fun fetchDetail(id: Int): PokemonDetail =
        client.get("$BASE/pokemon/$id").body()
}

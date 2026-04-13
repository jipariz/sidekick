package dev.parez.sidekick.demo

import dev.parez.sidekick.demo.db.PokemonCache
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PokemonRepository(
    private val api: PokemonApi,
    private val cache: PokemonCache,
) {
    private val fetchMutex = Mutex()
    private var nextOffset = 0
    var hasMore = true
        private set

    fun observePokemonList(): Flow<List<PokemonListEntry>> = cache.observeAll()

    fun observeDetail(id: Int): Flow<PokemonDetail?> = cache.observeDetail(id)

    suspend fun fetchNextPage(limit: Int = 20) {
        fetchMutex.withLock {
            if (!hasMore) return
            val response = api.fetchList(nextOffset, limit)
            cache.saveListEntries(response.results)
            nextOffset += limit
            hasMore = response.next != null
        }
    }

    suspend fun fetchDetail(id: Int) {
        val detail = api.fetchDetail(id)
        cache.saveDetail(detail)
    }
}
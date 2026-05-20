package dev.parez.sidekick.demo

import co.touchlab.kermit.Logger
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
            Logger.d("Repository") { "fetchNextPage: offset=$nextOffset, limit=$limit" }
            val response = api.fetchList(nextOffset, limit)
            cache.saveListEntries(response.results)
            Logger.i("Repository") { "fetchNextPage: cached ${response.results.size} entries" }
            nextOffset += limit
            hasMore = response.next != null
        }
    }

    suspend fun fetchDetail(id: Int) {
        Logger.d("Repository") { "fetchDetail: id=$id" }
        val detail = api.fetchDetail(id)
        cache.saveDetail(detail)
        Logger.i("Repository") { "fetchDetail: cached ${detail.name}" }
    }
}
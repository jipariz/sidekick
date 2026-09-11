package dev.parez.sidekick.demo.db

import androidx.room3.RoomDatabase
import dev.parez.sidekick.demo.PokemonDetail
import dev.parez.sidekick.demo.PokemonListEntry
import kotlinx.coroutines.flow.Flow

interface PokemonCache {
    fun observeAll(): Flow<List<PokemonListEntry>>

    fun observeDetail(id: Int): Flow<PokemonDetail?>

    suspend fun saveListEntries(entries: List<PokemonListEntry>)

    suspend fun saveDetail(detail: PokemonDetail)

    /**
     * The backing Room database, handed to Sidekick's database inspector so it can browse this
     * cache. Null for implementations with nothing to inspect. Exposed rather than re-opened: a
     * second connection to the same file would be a second writer.
     */
    val inspectableDatabase: RoomDatabase?
        get() = null
}

expect fun createPokemonCache(): PokemonCache

package dev.parez.sidekick.demo.db

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PokemonCacheDao {

    @Query("SELECT * FROM pokemon_list ORDER BY id ASC")
    fun observeListEntries(): Flow<List<PokemonListEntity>>

    @Query("SELECT * FROM pokemon_detail WHERE id = :id")
    fun observeDetail(id: Int): Flow<PokemonDetailEntity?>

    @Upsert
    suspend fun upsertListEntries(entities: List<PokemonListEntity>)

    @Upsert
    suspend fun upsertDetail(entity: PokemonDetailEntity)
}

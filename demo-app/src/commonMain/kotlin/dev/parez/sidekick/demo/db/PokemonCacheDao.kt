package dev.parez.sidekick.demo.db

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PokemonCacheDao {

    @Query("SELECT * FROM pokemon ORDER BY id ASC")
    fun observeAll(): Flow<List<PokemonEntity>>

    @Query("SELECT * FROM pokemon WHERE id = :id")
    fun observeById(id: Int): Flow<PokemonEntity?>

    @Upsert
    suspend fun upsertAll(entities: List<PokemonEntity>)

    @Upsert
    suspend fun upsert(entity: PokemonEntity)
}

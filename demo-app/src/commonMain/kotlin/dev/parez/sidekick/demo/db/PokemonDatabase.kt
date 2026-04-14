package dev.parez.sidekick.demo.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(
    entities = [PokemonEntity::class],
    version = 2,
)
@ConstructedBy(PokemonDatabaseConstructor::class)
abstract class PokemonDatabase : RoomDatabase() {
    abstract fun pokemonCacheDao(): PokemonCacheDao
}

@Suppress("KotlinNoActualForExpect", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA")
expect object PokemonDatabaseConstructor : RoomDatabaseConstructor<PokemonDatabase>

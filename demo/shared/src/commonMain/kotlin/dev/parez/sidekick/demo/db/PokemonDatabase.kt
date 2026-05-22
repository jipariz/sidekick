package dev.parez.sidekick.demo.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(
    entities = [PokemonListEntity::class, PokemonDetailEntity::class],
    version = 3,
)
@ConstructedBy(PokemonDatabaseConstructor::class)
abstract class PokemonDatabase : RoomDatabase() {
    abstract fun pokemonCacheDao(): PokemonCacheDao
}

// Under Kotlin 2.3+, the expect object must explicitly declare the override of
// `initialize()` so the metadata pass can verify the abstract member is accounted
// for (Room's KSP generates the actual impl per target — Android/JVM/JS/wasmJs).
@Suppress("KotlinNoActualForExpect", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA")
expect object PokemonDatabaseConstructor : RoomDatabaseConstructor<PokemonDatabase> {
    override fun initialize(): PokemonDatabase
}

package dev.parez.sidekick.demo.db

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

actual fun createPokemonCache(): PokemonCache {
    val dbFile = File(System.getProperty("user.home"), ".sidekick-demo/pokemon_cache.db")
    dbFile.parentFile?.mkdirs()
    val database = Room.databaseBuilder<PokemonDatabase>(
        name = dbFile.absolutePath,
    )
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(true)
        .build()
    return RoomPokemonCache(database.pokemonCacheDao())
}

package dev.parez.sidekick.demo.db

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.parez.sidekick.plugin.ApplicationContextHolder

actual fun createPokemonCache(): PokemonCache {
    val context = ApplicationContextHolder.context
    val dbFile = context.getDatabasePath("pokemon_cache.db")
    val database = Room.databaseBuilder<PokemonDatabase>(
        context = context,
        name = dbFile.absolutePath,
    )
        .setDriver(BundledSQLiteDriver())
        .build()
    return RoomPokemonCache(database.pokemonCacheDao())
}

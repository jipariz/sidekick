package dev.parez.sidekick.demo.db

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual fun createPokemonCache(): PokemonCache {
    val docsUrl: NSURL =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        ) ?: error("Failed to resolve iOS Documents directory for Room database")
    val dbPath =
        requireNotNull(docsUrl.URLByAppendingPathComponent("pokemon_cache.db")?.path) {
            "Failed to build iOS Room database path"
        }
    val database =
        Room.databaseBuilder<PokemonDatabase>(name = dbPath)
            .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration(true)
            .build()
    return RoomPokemonCache(database.pokemonCacheDao())
}

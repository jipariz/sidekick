package dev.parez.sidekick.demo.db

import androidx.room3.Room
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import org.w3c.dom.Worker

actual fun createPokemonCache(): PokemonCache {
    val driver = WebWorkerSQLiteDriver(
        Worker(js("""new URL("sqlite-wasm-worker/worker.js", import.meta.url)"""))
    )
    val database = Room.inMemoryDatabaseBuilder<PokemonDatabase>()
        .setDriver(driver)
        .build()
    return RoomPokemonCache(database.pokemonCacheDao())
}

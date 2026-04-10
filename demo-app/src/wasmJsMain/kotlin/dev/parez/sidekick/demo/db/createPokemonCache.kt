package dev.parez.sidekick.demo.db

import androidx.room3.Room
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver

actual fun createPokemonCache(): PokemonCache {
    val driver = WebWorkerSQLiteDriver(jsWorker())
    val database = Room.inMemoryDatabaseBuilder<PokemonDatabase>()
        .setDriver(driver)
        .build()
    return RoomPokemonCache(database.pokemonCacheDao())
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsWorker(): org.w3c.dom.Worker =
    js("""new Worker(new URL("sqlite-wasm-worker/worker.js", import.meta.url))""")

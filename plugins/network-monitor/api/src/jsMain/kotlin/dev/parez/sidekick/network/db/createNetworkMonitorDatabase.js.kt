package dev.parez.sidekick.network.db

import androidx.room3.Room
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import org.w3c.dom.Worker

// Web persistence requires the consumer's web app to bundle a sqlite-web
// worker as the npm package `sqlite-wasm-worker` (the same one demo/webApp
// uses for its Pokemon cache). See CLAUDE.md → "Consumer setup: web
// persistence" for the worker.js contents and webpack wiring.
//
// If the worker isn't bundled, `Worker(URL(...))` itself doesn't throw —
// the failure surfaces on the first SQL operation. NetworkMonitorStore.init
// guards against that with a probe try/catch and falls back to the in-memory
// list, so consumers that skip the web setup still see live monitor data,
// just without page-reload persistence.
internal actual fun createNetworkMonitorDatabase(): NetworkMonitorDatabase? {
    val driver =
        WebWorkerSQLiteDriver(
            Worker(js("""new URL("sqlite-wasm-worker/worker.js", import.meta.url)"""))
        )
    return Room.inMemoryDatabaseBuilder<NetworkMonitorDatabase>().setDriver(driver).build()
}

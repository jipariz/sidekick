package dev.parez.sidekick.network.db

import androidx.room3.Room
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver

// See createNetworkMonitorDatabase.js.kt — same setup, but wasmJs interop
// returns the Worker via a JS function rather than constructing it from
// Kotlin. NetworkMonitorStore.init probes the DB and falls back to in-memory
// if the consumer hasn't bundled the worker.
internal actual fun createNetworkMonitorDatabase(): NetworkMonitorDatabase? {
    val driver = WebWorkerSQLiteDriver(jsWorker())
    return Room.inMemoryDatabaseBuilder<NetworkMonitorDatabase>().setDriver(driver).build()
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsWorker(): org.w3c.dom.Worker =
    js("""new Worker(new URL("sqlite-wasm-worker/worker.js", import.meta.url))""")

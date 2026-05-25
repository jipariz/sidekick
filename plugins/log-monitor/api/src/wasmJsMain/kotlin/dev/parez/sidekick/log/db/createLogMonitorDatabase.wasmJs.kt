package dev.parez.sidekick.log.db

import androidx.room3.Room
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver

internal actual fun createLogMonitorDatabase(): LogMonitorDatabase? {
    val driver = WebWorkerSQLiteDriver(jsWorker())
    return Room.inMemoryDatabaseBuilder<LogMonitorDatabase>().setDriver(driver).build()
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsWorker(): org.w3c.dom.Worker =
    js("""new Worker(new URL("sqlite-wasm-worker/worker.js", import.meta.url))""")

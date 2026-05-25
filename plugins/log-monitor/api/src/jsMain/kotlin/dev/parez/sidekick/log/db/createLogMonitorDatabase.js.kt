package dev.parez.sidekick.log.db

import androidx.room3.Room
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import org.w3c.dom.Worker

// See createNetworkMonitorDatabase.js.kt — same pattern.
internal actual fun createLogMonitorDatabase(): LogMonitorDatabase? {
    val driver =
        WebWorkerSQLiteDriver(
            Worker(js("""new URL("sqlite-wasm-worker/worker.js", import.meta.url)"""))
        )
    return Room.inMemoryDatabaseBuilder<LogMonitorDatabase>().setDriver(driver).build()
}

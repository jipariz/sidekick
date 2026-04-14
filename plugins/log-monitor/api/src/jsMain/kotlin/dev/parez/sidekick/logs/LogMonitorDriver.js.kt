package dev.parez.sidekick.logs

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import dev.parez.sidekick.logs.db.LogMonitorDatabase
import org.w3c.dom.Worker

internal actual suspend fun createLogMonitorDriver(): SqlDriver? =
    WebWorkerDriver(
        Worker(js("""new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url)"""))
    ).also { LogMonitorDatabase.Schema.create(it).await() }

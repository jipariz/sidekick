package dev.parez.sidekick.network

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import dev.parez.sidekick.network.db.NetworkMonitorDatabase
import org.w3c.dom.Worker

internal actual suspend fun createNetworkMonitorDriver(): SqlDriver? =
    WebWorkerDriver(
        Worker(js("""new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url)"""))
    ).also { NetworkMonitorDatabase.Schema.create(it).await() }

package dev.parez.sidekick.logs

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.parez.sidekick.logs.db.LogMonitorDatabase

internal actual suspend fun createLogMonitorDriver(): SqlDriver? =
    NativeSqliteDriver(
        schema = LogMonitorDatabase.Schema.synchronous(),
        name = "log_monitor.db",
    )

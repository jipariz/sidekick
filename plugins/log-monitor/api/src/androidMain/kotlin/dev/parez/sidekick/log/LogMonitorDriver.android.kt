package dev.parez.sidekick.log

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.parez.sidekick.log.db.LogMonitorDatabase
import dev.parez.sidekick.plugin.ApplicationContextHolder

internal actual suspend fun createLogMonitorDriver(): SqlDriver? =
    AndroidSqliteDriver(
        schema = LogMonitorDatabase.Schema.synchronous(),
        context = ApplicationContextHolder.context,
        name = "log_monitor.db",
    )

package dev.parez.sidekick.logs

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.parez.sidekick.logs.db.LogMonitorDatabase

internal actual suspend fun createLogMonitorDriver(): SqlDriver? =
    JdbcSqliteDriver("jdbc:sqlite:log_monitor.db").also {
        LogMonitorDatabase.Schema.synchronous().create(it)
    }

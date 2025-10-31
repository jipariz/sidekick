package dev.parez.sidekick.network

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.parez.sidekick.network.db.NetworkMonitorDatabase

internal actual suspend fun createNetworkMonitorDriver(): SqlDriver? =
    JdbcSqliteDriver("jdbc:sqlite:network_monitor.db").also {
        NetworkMonitorDatabase.Schema.synchronous().create(it)
    }

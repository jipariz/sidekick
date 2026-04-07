package dev.parez.sidekick.network

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.parez.sidekick.network.db.NetworkMonitorDatabase

internal actual suspend fun createNetworkMonitorDriver(): SqlDriver? =
    AndroidSqliteDriver(
        schema = NetworkMonitorDatabase.Schema.synchronous(),
        context = ApplicationContextHolder.context,
        name = "network_monitor.db",
    )

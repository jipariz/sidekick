package dev.parez.sidekick.network

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.parez.sidekick.network.db.NetworkMonitorDatabase

internal actual suspend fun createNetworkMonitorDriver(): SqlDriver? =
    NativeSqliteDriver(
        schema = NetworkMonitorDatabase.Schema.synchronous(),
        name = "network_monitor.db",
    )

package dev.parez.sidekick.network

import app.cash.sqldelight.db.SqlDriver

internal expect suspend fun createNetworkMonitorDriver(): SqlDriver?

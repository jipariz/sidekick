package dev.parez.sidekick.network

import app.cash.sqldelight.db.SqlDriver

// SQLDelight web-worker-driver does not publish a wasmJs variant.
// Returning null signals NetworkMonitorStore to use in-memory storage instead.
internal actual suspend fun createNetworkMonitorDriver(): SqlDriver? = null

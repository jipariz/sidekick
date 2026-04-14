package dev.parez.sidekick.logs

import app.cash.sqldelight.db.SqlDriver

// SQLDelight web-worker-driver does not publish a wasmJs variant.
// Returning null signals LogMonitorStore to use in-memory storage instead.
internal actual suspend fun createLogMonitorDriver(): SqlDriver? = null

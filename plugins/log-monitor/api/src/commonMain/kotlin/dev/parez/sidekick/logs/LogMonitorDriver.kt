package dev.parez.sidekick.logs

import app.cash.sqldelight.db.SqlDriver

internal expect suspend fun createLogMonitorDriver(): SqlDriver?

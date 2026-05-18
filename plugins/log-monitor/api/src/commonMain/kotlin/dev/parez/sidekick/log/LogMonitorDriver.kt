package dev.parez.sidekick.log

import app.cash.sqldelight.db.SqlDriver

internal expect suspend fun createLogMonitorDriver(): SqlDriver?

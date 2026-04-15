package dev.parez.sidekick.logs

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.parez.sidekick.logs.db.LogMonitorDatabase
import java.io.File

internal actual suspend fun createLogMonitorDriver(): SqlDriver? {
    val file = File(System.getProperty("java.io.tmpdir"), "sidekick_log_monitor.db")
    val driver = JdbcSqliteDriver("jdbc:sqlite:$file")
    val schema = LogMonitorDatabase.Schema.synchronous()
    val schemaVersion = LogMonitorDatabase.Schema.version
    val currentVersion = driver.executeQuery<Long>(
        identifier = null,
        sql = "PRAGMA user_version",
        mapper = { QueryResult.Value(it.getLong(0) ?: 0L) },
        parameters = 0,
    ).value
    when {
        currentVersion == 0L -> {
            // user_version=0 means either a brand-new file or a legacy file created before
            // versioning was added. Check sqlite_master to distinguish the two cases.
            val tableExists = driver.executeQuery<Long>(
                identifier = null,
                sql = "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='LogEntry'",
                mapper = { QueryResult.Value(it.getLong(0) ?: 0L) },
                parameters = 0,
            ).value > 0L
            if (!tableExists) schema.create(driver)
            driver.execute(null, "PRAGMA user_version = $schemaVersion", 0)
        }
        currentVersion < schemaVersion ->
            schema.migrate(driver, currentVersion, schemaVersion)
    }
    return driver
}

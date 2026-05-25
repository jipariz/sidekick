package dev.parez.sidekick.log.db

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.parez.sidekick.plugin.ApplicationContextHolder

internal actual fun createLogMonitorDatabase(): LogMonitorDatabase? {
    val context = ApplicationContextHolder.context
    val dbFile = context.getDatabasePath("sidekick_log_monitor.db")
    return Room.databaseBuilder<LogMonitorDatabase>(
            context = context,
            name = dbFile.absolutePath,
        )
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(true)
        .build()
}

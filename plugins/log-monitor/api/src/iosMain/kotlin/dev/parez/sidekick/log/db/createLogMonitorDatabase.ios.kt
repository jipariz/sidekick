package dev.parez.sidekick.log.db

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
internal actual fun createLogMonitorDatabase(): LogMonitorDatabase? {
    val docsUrl: NSURL =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        ) ?: error("Failed to resolve iOS Documents directory for Room database")
    val dbPath =
        requireNotNull(docsUrl.URLByAppendingPathComponent("sidekick_log_monitor.db")?.path) {
            "Failed to build iOS Room database path"
        }
    return Room.databaseBuilder<LogMonitorDatabase>(name = dbPath)
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(true)
        .build()
}

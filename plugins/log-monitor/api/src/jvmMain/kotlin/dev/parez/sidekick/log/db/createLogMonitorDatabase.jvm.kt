package dev.parez.sidekick.log.db

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

internal actual fun createLogMonitorDatabase(): LogMonitorDatabase? {
    // `_room` suffix avoids colliding with the pre-migration SQLDelight file —
    // see the matching comment in createNetworkMonitorDatabase.jvm.kt.
    val dbFile = File(System.getProperty("java.io.tmpdir"), "sidekick_log_monitor_room.db")
    dbFile.parentFile?.mkdirs()
    return Room.databaseBuilder<LogMonitorDatabase>(name = dbFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(true)
        .build()
}

package dev.parez.sidekick.network.db

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

internal actual fun createNetworkMonitorDatabase(): NetworkMonitorDatabase? {
    // The `_room` suffix avoids colliding with the pre-migration SQLDelight DB
    // file at the same tmpdir path — Room can't parse SQLDelight's schema and
    // doesn't drop the file on validation failure, even with
    // fallbackToDestructiveMigration. Picking a distinct name guarantees a
    // clean open on consumers that ran an earlier Sidekick build.
    val dbFile = File(System.getProperty("java.io.tmpdir"), "sidekick_network_monitor_room.db")
    dbFile.parentFile?.mkdirs()
    return Room.databaseBuilder<NetworkMonitorDatabase>(name = dbFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(true)
        .build()
}

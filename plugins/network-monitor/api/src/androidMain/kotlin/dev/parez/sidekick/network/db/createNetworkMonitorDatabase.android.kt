package dev.parez.sidekick.network.db

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.parez.sidekick.plugin.ApplicationContextHolder

internal actual fun createNetworkMonitorDatabase(): NetworkMonitorDatabase? {
    val context = ApplicationContextHolder.context
    val dbFile = context.getDatabasePath("sidekick_network_monitor.db")
    return Room.databaseBuilder<NetworkMonitorDatabase>(
            context = context,
            name = dbFile.absolutePath,
        )
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(true)
        .build()
}

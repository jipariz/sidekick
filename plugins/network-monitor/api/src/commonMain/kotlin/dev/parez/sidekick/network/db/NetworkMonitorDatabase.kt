package dev.parez.sidekick.network.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(entities = [NetworkCallEntity::class], version = 1)
@ConstructedBy(NetworkMonitorDatabaseConstructor::class)
internal abstract class NetworkMonitorDatabase : RoomDatabase() {
    abstract fun networkCallDao(): NetworkCallDao
}

// Kotlin 2.3+ requires the expect object to explicitly declare the abstract
// `initialize()` override so the metadata pass passes; the actuals are
// generated per target by Room's KSP.
@Suppress("KotlinNoActualForExpect", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA")
internal expect object NetworkMonitorDatabaseConstructor :
    RoomDatabaseConstructor<NetworkMonitorDatabase> {
    override fun initialize(): NetworkMonitorDatabase
}

internal expect fun createNetworkMonitorDatabase(): NetworkMonitorDatabase?

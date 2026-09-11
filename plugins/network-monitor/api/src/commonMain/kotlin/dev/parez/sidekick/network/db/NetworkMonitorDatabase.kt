package dev.parez.sidekick.network.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

/**
 * Schema 2 added `bodiesEvicted`.
 *
 * The persistent builders keep `fallbackToDestructiveMigration`, so an upgrade drops captured calls
 * rather than migrating them. That is deliberate, not an oversight: this table is an ephemeral
 * debug capture — purged on an hourly retention sweep and capped at 500 rows — so a migration would
 * preserve data that is minutes from being deleted anyway, at the cost of owning a migration chain
 * for a throwaway cache. (A hand-written `ALTER TABLE` also cannot live in commonMain: neither
 * `execSQL` nor `prepare` is part of androidx.sqlite's common metadata.)
 */
@Database(entities = [NetworkCallEntity::class], version = 2)
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

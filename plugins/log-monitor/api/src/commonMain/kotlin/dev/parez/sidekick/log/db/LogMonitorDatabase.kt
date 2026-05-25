package dev.parez.sidekick.log.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(entities = [LogEntryEntity::class], version = 1)
@ConstructedBy(LogMonitorDatabaseConstructor::class)
internal abstract class LogMonitorDatabase : RoomDatabase() {
    abstract fun logEntryDao(): LogEntryDao
}

@Suppress("KotlinNoActualForExpect", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA")
internal expect object LogMonitorDatabaseConstructor : RoomDatabaseConstructor<LogMonitorDatabase> {
    override fun initialize(): LogMonitorDatabase
}

internal expect fun createLogMonitorDatabase(): LogMonitorDatabase?

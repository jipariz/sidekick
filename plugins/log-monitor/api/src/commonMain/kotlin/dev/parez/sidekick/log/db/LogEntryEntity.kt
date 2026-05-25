package dev.parez.sidekick.log.db

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "log_entries")
internal data class LogEntryEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val level: String,
    val tag: String,
    val message: String,
    val throwable: String?,
    val metadata: String?,
)

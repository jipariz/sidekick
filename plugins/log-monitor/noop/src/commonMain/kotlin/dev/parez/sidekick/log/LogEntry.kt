package dev.parez.sidekick.log

import androidx.compose.runtime.Immutable

@Immutable
public data class LogEntry(
    val id: String,
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: String?,
    val metadata: Map<String, String>? = null,
)

public enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    ASSERT,
}

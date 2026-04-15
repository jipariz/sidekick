package dev.parez.sidekick.logs

data class LogEntry(
    val id: String,
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: String?,
    val metadata: Map<String, String>? = null,
)

enum class LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR, ASSERT }

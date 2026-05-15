package dev.parez.sidekick.logs

import androidx.compose.runtime.Immutable

@Immutable
data class LogFilter(
    val query: String = "",
    val levels: Set<LogLevel> = LogLevel.entries.toSet(),
) {
    fun matches(entry: LogEntry): Boolean {
        if (entry.level !in levels) return false
        if (query.isNotBlank() &&
            !entry.tag.contains(query, ignoreCase = true) &&
            !entry.message.contains(query, ignoreCase = true)
        ) return false
        return true
    }

    fun toLikeToken(): String {
        if (query.isBlank()) return "%"
        val escaped = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        return "%$escaped%"
    }

    fun allLevelsSelected(): Boolean = levels.size == LogLevel.entries.size
}

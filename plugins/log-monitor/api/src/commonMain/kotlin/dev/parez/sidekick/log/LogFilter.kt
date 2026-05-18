package dev.parez.sidekick.log

import androidx.compose.runtime.Immutable

@Immutable
data class LogFilter(
    val query: String = "",
    val levels: Set<LogLevel> = emptySet(),
) {
    fun matches(entry: LogEntry): Boolean {
        if (query.isNotBlank() &&
            !entry.tag.contains(query, ignoreCase = true) &&
            !entry.message.contains(query, ignoreCase = true)
        ) return false
        if (levels.isNotEmpty() && entry.level !in levels) return false
        return true
    }

    fun toLikeToken(): String {
        if (query.isBlank()) return "%"
        val escaped = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        return "%$escaped%"
    }
}

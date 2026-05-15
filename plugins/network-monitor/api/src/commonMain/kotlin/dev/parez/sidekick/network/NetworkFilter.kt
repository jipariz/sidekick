package dev.parez.sidekick.network

import androidx.compose.runtime.Immutable

@Immutable
data class NetworkFilter(
    val query: String = "",
    val methods: Set<String> = emptySet(),
) {
    fun matches(call: NetworkCall): Boolean {
        if (query.isNotBlank() &&
            !call.url.contains(query, ignoreCase = true) &&
            !call.method.contains(query, ignoreCase = true)
        ) return false
        if (methods.isNotEmpty() && call.method.uppercase() !in methods) return false
        return true
    }

    fun toLikeToken(): String {
        if (query.isBlank()) return "%"
        val escaped = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        return "%$escaped%"
    }
}

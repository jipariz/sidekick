package dev.parez.sidekick.network

import androidx.compose.runtime.Immutable

@Immutable
data class NetworkFilter(
    val query: String = "",
    val methods: Set<String> = emptySet(),
) {
    fun matches(call: NetworkCall): Boolean = false

    fun toLikeToken(): String = "%"
}

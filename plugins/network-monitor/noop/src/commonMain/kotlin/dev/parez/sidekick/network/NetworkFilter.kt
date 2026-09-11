package dev.parez.sidekick.network

import androidx.compose.runtime.Immutable

@Immutable
public data class NetworkFilter(val query: String = "", val methods: Set<String> = emptySet()) {
    public fun matches(call: NetworkCall): Boolean = false

    public fun toLikeToken(): String = "%"
}

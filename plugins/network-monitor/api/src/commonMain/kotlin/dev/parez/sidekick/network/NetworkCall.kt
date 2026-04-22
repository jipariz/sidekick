package dev.parez.sidekick.network

import androidx.compose.runtime.Immutable

@Immutable
data class NetworkCall(
    val id: String,
    val url: String,
    val method: String,
    val requestHeaders: Map<String, String>,
    val requestBody: String?,
    val requestTimestamp: Long,
    val responseCode: Int?,
    val responseHeaders: Map<String, String>,
    val responseBody: String?,
    val responseTimestamp: Long?,
    val error: String?,
    val status: CallStatus,
) {
    val durationMs: Long?
        get() = if (responseTimestamp != null) responseTimestamp - requestTimestamp else null
}

enum class CallStatus { PENDING, COMPLETE, ERROR }

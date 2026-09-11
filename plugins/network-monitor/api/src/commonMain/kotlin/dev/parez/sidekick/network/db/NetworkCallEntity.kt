package dev.parez.sidekick.network.db

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "network_calls")
internal data class NetworkCallEntity(
    @PrimaryKey val id: String,
    val url: String,
    val method: String,
    val requestHeaders: String,
    val requestBody: String?,
    val requestTimestamp: Long,
    val responseCode: Int?,
    val responseHeaders: String,
    val responseBody: String?,
    val responseTimestamp: Long?,
    val error: String?,
    val status: String,
    /** True when body text was dropped to stay under the store's aggregate body budget. */
    val bodiesEvicted: Boolean = false,
)

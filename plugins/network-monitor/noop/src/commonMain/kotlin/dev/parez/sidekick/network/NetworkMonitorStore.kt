package dev.parez.sidekick.network

import androidx.paging.PagingData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Release-variant stub for `NetworkMonitorStore` — keeps the same public surface
 * but performs no recording and opens no database. All `recordX` calls are no-ops;
 * all query flows emit empty results.
 */
@Suppress("UNUSED_PARAMETER")
class NetworkMonitorStore(scope: CoroutineScope? = null) {

    fun init(retentionPeriod: Duration = 1.hours) = Unit

    fun pagedCalls(filter: Flow<NetworkFilter>): Flow<PagingData<NetworkCall>> =
        flowOf(PagingData.empty())

    fun filteredCount(filter: Flow<NetworkFilter>): Flow<Long> = flowOf(0L)

    fun callById(id: String): Flow<NetworkCall?> = flowOf(null)

    suspend fun recordRequest(
        id: String,
        url: String,
        method: String,
        headers: Map<String, String>,
        body: String?,
        timestamp: Long,
    ) = Unit

    suspend fun recordResponse(
        id: String,
        code: Int,
        headers: Map<String, String>,
        timestamp: Long,
    ) = Unit

    suspend fun recordResponseBody(id: String, body: String) = Unit

    suspend fun recordError(id: String, error: Throwable) = Unit

    suspend fun clear() = Unit
}

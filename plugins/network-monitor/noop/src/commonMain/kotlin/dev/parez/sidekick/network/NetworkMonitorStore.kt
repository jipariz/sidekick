package dev.parez.sidekick.network

import androidx.paging.PagingData
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Release-variant stub for `NetworkMonitorStore` — keeps the same public surface but performs no
 * recording and opens no database. All `recordX` calls are no-ops; all query flows emit empty
 * results.
 */
@Suppress("UNUSED_PARAMETER")
public class NetworkMonitorStore(scope: CoroutineScope? = null) {

    public fun init(
        retentionPeriod: Duration = 1.hours,
        bodyBudgetChars: Int = BodyBudget.Default,
    ): Unit = Unit

    public fun pagedCalls(filter: Flow<NetworkFilter>): Flow<PagingData<NetworkCall>> =
        flowOf(PagingData.empty())

    public fun filteredCount(filter: Flow<NetworkFilter>): Flow<Long> = flowOf(0L)

    public fun callById(id: String): Flow<NetworkCall?> = flowOf(null)

    public suspend fun recordRequest(
        id: String,
        url: String,
        method: String,
        headers: Map<String, String>,
        body: String?,
        timestamp: Long,
    ): Unit = Unit

    public suspend fun recordResponse(
        id: String,
        code: Int,
        headers: Map<String, String>,
        timestamp: Long,
    ): Unit = Unit

    public suspend fun recordResponseBody(id: String, body: String): Unit = Unit

    public suspend fun recordError(id: String, error: Throwable): Unit = Unit

    public suspend fun exportAll(filter: NetworkFilter): List<NetworkCall> = emptyList()

    public suspend fun clear(): Unit = Unit
}

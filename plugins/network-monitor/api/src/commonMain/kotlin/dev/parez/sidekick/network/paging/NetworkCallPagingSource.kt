package dev.parez.sidekick.network.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.cash.sqldelight.Query
import app.cash.sqldelight.async.coroutines.awaitAsList
import dev.parez.sidekick.network.CallStatus
import dev.parez.sidekick.network.NetworkCall
import dev.parez.sidekick.network.NetworkFilter
import dev.parez.sidekick.network.db.NetworkMonitorDatabase
import dev.parez.sidekick.network.decodeToHeaderMap
import dev.parez.sidekick.network.db.NetworkCall as DbNetworkCall

internal class NetworkCallPagingSource(
    private val db: NetworkMonitorDatabase,
    private val filter: NetworkFilter,
) : PagingSource<Int, NetworkCall>() {

    private val listenerQuery: Query<*> = db.networkCallQueries.selectAll()
    private val listener = Query.Listener { invalidate() }

    init {
        listenerQuery.addListener(listener)
        registerInvalidatedCallback { listenerQuery.removeListener(listener) }
    }

    override val keyReuseSupported: Boolean = true
    override val jumpingSupported: Boolean = true

    override fun getRefreshKey(state: PagingState<Int, NetworkCall>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, NetworkCall> {
        val offset = params.key ?: 0
        val limit = params.loadSize.toLong()
        val token = filter.toLikeToken()
        return try {
            val rows = if (filter.methods.isEmpty()) {
                db.networkCallQueries
                    .selectPagedFilteredAllMethods(token, limit, offset.toLong())
                    .awaitAsList()
            } else {
                db.networkCallQueries
                    .selectPagedFiltered(token, filter.methods, limit, offset.toLong())
                    .awaitAsList()
            }
            val mapped = rows.map { it.toDomain() }
            LoadResult.Page(
                data = mapped,
                prevKey = if (offset == 0) null else (offset - params.loadSize).coerceAtLeast(0),
                nextKey = if (mapped.size < params.loadSize) null else offset + mapped.size,
            )
        } catch (t: Throwable) {
            LoadResult.Error(t)
        }
    }
}

internal fun DbNetworkCall.toDomain() = NetworkCall(
    id = id,
    url = url,
    method = method,
    requestHeaders = requestHeaders.decodeToHeaderMap(),
    requestBody = requestBody,
    requestTimestamp = requestTimestamp,
    responseCode = responseCode?.toInt(),
    responseHeaders = responseHeaders.decodeToHeaderMap(),
    responseBody = responseBody,
    responseTimestamp = responseTimestamp,
    error = error,
    status = when (status) {
        "COMPLETE" -> CallStatus.COMPLETE
        "ERROR" -> CallStatus.ERROR
        else -> CallStatus.PENDING
    },
)

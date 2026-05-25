package dev.parez.sidekick.network.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import dev.parez.sidekick.network.NetworkCall
import dev.parez.sidekick.network.NetworkFilter
import dev.parez.sidekick.network.db.NetworkMonitorDatabase
import dev.parez.sidekick.network.toDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal class NetworkCallPagingSource(
    private val db: NetworkMonitorDatabase,
    private val filter: NetworkFilter,
    scope: CoroutineScope,
) : PagingSource<Int, NetworkCall>() {

    // Room 3 KMP exposes change notifications via the invalidation tracker's
    // Flow API. The Android-only generated PagingSource normally wires this up
    // for you; here we do it by hand. The collector runs on the store's scope
    // and is cancelled either when this PagingSource invalidates or when the
    // store dies.
    private val invalidationJob: Job = scope.launch {
        db.invalidationTracker.createFlow(NETWORK_CALLS_TABLE, emitInitialState = false).collect {
            invalidate()
        }
    }

    init {
        registerInvalidatedCallback { invalidationJob.cancel() }
    }

    override val keyReuseSupported: Boolean = true
    override val jumpingSupported: Boolean = true

    override fun getRefreshKey(state: PagingState<Int, NetworkCall>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, NetworkCall> {
        val offset = params.key ?: 0
        val limit = params.loadSize
        val token = filter.toLikeToken()
        val methods = filter.methods.toList()
        val hasMethodFilter = if (methods.isEmpty()) 0 else 1
        return try {
            val rows =
                db.networkCallDao()
                    .loadPaged(
                        likeToken = token,
                        methods = methods,
                        hasMethodFilter = hasMethodFilter,
                        limit = limit,
                        offset = offset,
                    )
            val mapped = rows.map { it.toDomain() }
            LoadResult.Page(
                data = mapped,
                prevKey = if (offset == 0) null else (offset - params.loadSize).coerceAtLeast(0),
                nextKey = if (mapped.size < limit) null else offset + mapped.size,
            )
        } catch (t: Throwable) {
            LoadResult.Error(t)
        }
    }

    private companion object {
        const val NETWORK_CALLS_TABLE = "network_calls"
    }
}

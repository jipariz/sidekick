package dev.parez.sidekick.log.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import dev.parez.sidekick.log.LogEntry
import dev.parez.sidekick.log.LogFilter
import dev.parez.sidekick.log.db.LogMonitorDatabase
import dev.parez.sidekick.log.toDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal class LogEntryPagingSource(
    private val db: LogMonitorDatabase,
    private val filter: LogFilter,
    scope: CoroutineScope,
) : PagingSource<Int, LogEntry>() {

    private val invalidationJob: Job = scope.launch {
        db.invalidationTracker.createFlow(LOG_ENTRIES_TABLE, emitInitialState = false).collect {
            invalidate()
        }
    }

    init {
        registerInvalidatedCallback { invalidationJob.cancel() }
    }

    override val keyReuseSupported: Boolean = true
    override val jumpingSupported: Boolean = true

    override fun getRefreshKey(state: PagingState<Int, LogEntry>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, LogEntry> {
        val offset = params.key ?: 0
        val limit = params.loadSize
        val token = filter.toLikeToken()
        val levels = filter.levels.map { it.name }
        val hasLevelFilter = if (levels.isEmpty()) 0 else 1
        return try {
            val rows =
                db.logEntryDao()
                    .loadPaged(
                        likeToken = token,
                        levels = levels,
                        hasLevelFilter = hasLevelFilter,
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
        const val LOG_ENTRIES_TABLE = "log_entries"
    }
}

package dev.parez.sidekick.log.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.cash.sqldelight.Query
import app.cash.sqldelight.async.coroutines.awaitAsList
import dev.parez.sidekick.log.LogEntry
import dev.parez.sidekick.log.LogFilter
import dev.parez.sidekick.log.LogLevel
import dev.parez.sidekick.log.db.LogEntry as DbLogEntry
import dev.parez.sidekick.log.db.LogMonitorDatabase
import dev.parez.sidekick.log.decodeToMetadataMap

internal class LogEntryPagingSource(
    private val db: LogMonitorDatabase,
    private val filter: LogFilter,
) : PagingSource<Int, LogEntry>() {

    private val listenerQuery: Query<*> = db.logEntryQueries.selectAll()
    private val listener = Query.Listener { invalidate() }

    init {
        listenerQuery.addListener(listener)
        registerInvalidatedCallback { listenerQuery.removeListener(listener) }
    }

    override val keyReuseSupported: Boolean = true
    override val jumpingSupported: Boolean = true

    override fun getRefreshKey(state: PagingState<Int, LogEntry>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, LogEntry> {
        val offset = params.key ?: 0
        val limit = params.loadSize.toLong()
        val token = filter.toLikeToken()
        return try {
            val rows =
                if (filter.levels.isEmpty()) {
                    db.logEntryQueries
                        .selectPagedFilteredAllLevels(token, limit, offset.toLong())
                        .awaitAsList()
                } else {
                    val levelNames = filter.levels.map { it.name }.toSet()
                    db.logEntryQueries
                        .selectPagedFiltered(token, levelNames, limit, offset.toLong())
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

internal fun DbLogEntry.toDomain() =
    LogEntry(
        id = id,
        timestamp = timestamp,
        level = LogLevel.entries.firstOrNull { it.name == level } ?: LogLevel.DEBUG,
        tag = tag,
        message = message,
        throwable = throwable,
        metadata = metadata?.decodeToMetadataMap(),
    )

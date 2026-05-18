package dev.parez.sidekick.log

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Release-variant stub for `LogMonitorStore` — keeps the same public surface
 * but performs no recording and opens no database. `record` and `log` are
 * no-ops; all query flows emit empty results.
 */
@Suppress("UNUSED_PARAMETER")
object LogMonitorStore : LogCollector {

    fun init(retentionPeriod: Duration = 1.hours) = Unit

    fun pagedEntries(filter: Flow<LogFilter>): Flow<PagingData<LogEntry>> =
        flowOf(PagingData.empty())

    fun filteredCount(filter: Flow<LogFilter>): Flow<Long> = flowOf(0L)

    fun entryById(id: String): Flow<LogEntry?> = flowOf(null)

    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit

    fun record(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
        metadata: Map<String, String>? = null,
    ) = Unit

    suspend fun clear() = Unit
}

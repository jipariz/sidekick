package dev.parez.sidekick.log

import androidx.paging.PagingData
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Release-variant stub for `LogMonitorStore` — keeps the same public surface but performs no
 * recording and opens no database. `record` and `log` are no-ops; all query flows emit empty
 * results.
 */
@Suppress("UNUSED_PARAMETER")
public object LogMonitorStore : LogCollector {

    public fun init(retentionPeriod: Duration = 1.hours): Unit = Unit

    /** Release-variant stub — nothing is ever recorded, so the sequence never moves. */
    public val recordedCount: StateFlow<Long> = MutableStateFlow(0L).asStateFlow()

    public fun pagedEntries(filter: Flow<LogFilter>): Flow<PagingData<LogEntry>> =
        flowOf(PagingData.empty())

    public fun filteredCount(filter: Flow<LogFilter>): Flow<Long> = flowOf(0L)

    public fun entryById(id: String): Flow<LogEntry?> = flowOf(null)

    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?): Unit =
        Unit

    public fun record(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
        metadata: Map<String, String>? = null,
    ): Unit = Unit

    public suspend fun exportAll(filter: LogFilter): List<LogEntry> = emptyList()

    public suspend fun clear(): Unit = Unit
}

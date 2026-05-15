package dev.parez.sidekick.logs

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.parez.sidekick.logs.db.LogMonitorDatabase
import dev.parez.sidekick.logs.paging.LogEntryPagingSource
import dev.parez.sidekick.logs.paging.toDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

private const val MAX_ENTRIES = 1000L
private const val MAX_MESSAGE_LENGTH = 16_384

@OptIn(ExperimentalCoroutinesApi::class)
object LogMonitorStore : LogCollector {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _database = MutableStateFlow<LogMonitorDatabase?>(null)
    private val _inMemory = MutableStateFlow<List<LogEntry>?>(null)

    private val inMemorySnapshot: StateFlow<List<LogEntry>> = _inMemory
        .filterNotNull()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val initialized = MutableStateFlow(false)

    fun init(retentionPeriod: Duration = 1.hours) {
        if (!initialized.compareAndSet(expect = false, update = true)) return

        scope.launch {
            val driver = createLogMonitorDriver()
            if (driver != null) {
                val db = LogMonitorDatabase(driver)
                db.logEntryQueries.deleteOlderThan(currentTimeMillis() - retentionPeriod.inWholeMilliseconds)
                _database.value = db
            } else {
                _inMemory.value = emptyList()
            }
        }
    }

    fun pagedEntries(filter: Flow<LogFilter>): Flow<PagingData<LogEntry>> =
        combine(_database, filter.distinctUntilChanged()) { db, f -> db to f }
            .flatMapLatest { (db, f) ->
                if (db != null) {
                    Pager(
                        config = LogPagingConfig,
                        pagingSourceFactory = { LogEntryPagingSource(db, f) },
                    ).flow
                } else {
                    // In-memory fallback (wasmJs): the list is capped at MAX_ENTRIES
                    // so we don't need real pagination — just snapshot the filtered list
                    // each time it changes. PagingData.from() avoids the Pager + PagingSource
                    // machinery, which has interop issues on wasmJs.
                    inMemorySnapshot.map { list ->
                        PagingData.from(
                            data = list.filter(f::matches),
                            sourceLoadStates = StaticLoadStates,
                        )
                    }
                }
            }

    fun filteredCount(filter: Flow<LogFilter>): Flow<Long> =
        combine(_database, filter.distinctUntilChanged()) { db, f -> db to f }
            .flatMapLatest { (db, f) ->
                if (db != null) {
                    val token = f.toLikeToken()
                    if (f.levels.isEmpty()) {
                        db.logEntryQueries.countFilteredAllLevels(token)
                            .asFlow().mapToOne(Dispatchers.Default)
                    } else {
                        val levelNames = f.levels.map { it.name }.toSet()
                        db.logEntryQueries.countFiltered(token, levelNames)
                            .asFlow().mapToOne(Dispatchers.Default)
                    }
                } else {
                    inMemorySnapshot.map { list -> list.count(f::matches).toLong() }
                }
            }

    fun entryById(id: String): Flow<LogEntry?> = _database.flatMapLatest { db ->
        if (db != null) {
            db.logEntryQueries.selectById(id)
                .asFlow()
                .mapToOneOrNull(Dispatchers.Default)
                .map { it?.toDomain() }
        } else {
            inMemorySnapshot.map { list -> list.firstOrNull { it.id == id } }
        }
    }

    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val id = randomUuid()
        val timestamp = currentTimeMillis()
        val throwableStr = throwable?.stackTraceToString()
        scope.launch {
            record(id, timestamp, level, tag, message, throwableStr, null)
        }
    }

    fun record(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
        metadata: Map<String, String>? = null,
    ) {
        val id = randomUuid()
        val timestamp = currentTimeMillis()
        val throwableStr = throwable?.stackTraceToString()
        scope.launch {
            record(id, timestamp, level, tag, message, throwableStr, metadata)
        }
    }

    private suspend fun record(
        id: String,
        timestamp: Long,
        level: LogLevel,
        tag: String,
        message: String,
        throwable: String?,
        metadata: Map<String, String>?,
    ) {
        val db = _database.value
        if (db != null) {
            db.logEntryQueries.insertEntry(
                id = id,
                timestamp = timestamp,
                level = level.name,
                tag = tag,
                message = message.truncate(),
                throwable = throwable?.truncate(),
                metadata = metadata?.encodeToJson(),
            )
            trimDbIfNeeded(db)
        } else if (_inMemory.value != null) {
            val entry = LogEntry(
                id = id,
                timestamp = timestamp,
                level = level,
                tag = tag,
                message = message.truncate(),
                throwable = throwable?.truncate(),
                metadata = metadata,
            )
            _inMemory.update { list ->
                (listOf(entry) + (list ?: emptyList())).take(MAX_ENTRIES.toInt())
            }
        }
    }

    suspend fun clear() {
        _database.value?.logEntryQueries?.deleteAll()
        if (_inMemory.value != null) _inMemory.value = emptyList()
    }

    private suspend fun trimDbIfNeeded(db: LogMonitorDatabase) {
        val count = db.logEntryQueries.countAll().awaitAsOne()
        val over = count - MAX_ENTRIES
        if (over > 0) db.logEntryQueries.deleteOldestOverLimit(over)
    }

    private fun String.truncate() =
        if (length > MAX_MESSAGE_LENGTH) take(MAX_MESSAGE_LENGTH) + "…" else this

    internal val LogPagingConfig = PagingConfig(
        pageSize = 30,
        prefetchDistance = 15,
        initialLoadSize = 60,
        enablePlaceholders = false,
        maxSize = 300,
        jumpThreshold = 120,
    )

    // For PagingData.from() on the in-memory path: signal that the static list
    // is fully loaded so LazyPagingItems renders NotLoading instead of staying
    // in the default Loading state.
    private val StaticLoadStates = LoadStates(
        refresh = LoadState.NotLoading(endOfPaginationReached = true),
        prepend = LoadState.NotLoading(endOfPaginationReached = true),
        append = LoadState.NotLoading(endOfPaginationReached = true),
    )
}

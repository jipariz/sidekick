package dev.parez.sidekick.log

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import dev.parez.sidekick.log.db.LogEntryEntity
import dev.parez.sidekick.log.db.LogMonitorDatabase
import dev.parez.sidekick.log.db.createLogMonitorDatabase
import dev.parez.sidekick.log.paging.LogEntryPagingSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val MAX_ENTRIES = 1000L
private const val MAX_MESSAGE_LENGTH = 16_384

// Entries buffered between the (non-suspending) log call and the DB writer.
// Bounded, so logs recorded before init() — or faster than the writer drains —
// cost a fixed amount of memory rather than growing without limit.
private const val PENDING_CAPACITY = 4096

// Upper bound on rows per insertAll transaction.
private const val MAX_BATCH = 200

@OptIn(ExperimentalCoroutinesApi::class)
public object LogMonitorStore : LogCollector {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _database = MutableStateFlow<LogMonitorDatabase?>(null)
    private val _inMemory = MutableStateFlow<List<LogEntry>?>(null)

    private val inMemorySnapshot: StateFlow<List<LogEntry>> =
        _inMemory.filterNotNull().stateIn(scope, SharingStarted.Eagerly, emptyList())

    // Producers hand entries off here and return immediately; a single writer
    // coroutine drains the channel and writes in batches. DROP_OLDEST means a
    // pathological burst loses the oldest pending lines rather than the host app.
    private val pending =
        Channel<LogEntryEntity>(
            capacity = PENDING_CAPACITY,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    private val initialized = MutableStateFlow(false)

    public fun init(retentionPeriod: Duration = 1.hours) {
        if (!initialized.compareAndSet(expect = false, update = true)) return

        scope.launch {
            val db = createLogMonitorDatabase()
            val ready =
                db != null &&
                    runCatching {
                            // See NetworkMonitorStore.init — on web this probes
                            // for a missing sqlite-web worker.
                            db.logEntryDao()
                                .deleteOlderThan(
                                    currentTimeMillis() - retentionPeriod.inWholeMilliseconds
                                )
                        }
                        .isSuccess
            if (ready) {
                _database.value = db
            } else {
                _inMemory.value = emptyList()
            }
            // Started only once storage is resolved. Anything logged before this
            // point waited in `pending` and is flushed by the first drain.
            launchWriter()
        }
    }

    private fun CoroutineScope.launchWriter() = launch {
        val batch = ArrayList<LogEntryEntity>(MAX_BATCH)
        while (isActive) {
            batch.clear()
            // Suspends until there is at least one entry, then takes whatever else
            // has already queued up without waiting for more.
            batch += pending.receive()
            while (batch.size < MAX_BATCH) {
                batch += pending.tryReceive().getOrNull() ?: break
            }
            writeBatch(batch)
        }
    }

    public fun pagedEntries(filter: Flow<LogFilter>): Flow<PagingData<LogEntry>> =
        combine(_database, filter.distinctUntilChanged()) { db, f -> db to f }
            .flatMapLatest { (db, f) ->
                if (db != null) {
                    Pager(
                            config = LogPagingConfig,
                            pagingSourceFactory = { LogEntryPagingSource(db, f, scope) },
                        )
                        .flow
                } else {
                    inMemorySnapshot.map { list ->
                        PagingData.from(
                            data = list.filter(f::matches),
                            sourceLoadStates = StaticLoadStates,
                        )
                    }
                }
            }

    public fun filteredCount(filter: Flow<LogFilter>): Flow<Long> =
        combine(_database, filter.distinctUntilChanged()) { db, f -> db to f }
            .flatMapLatest { (db, f) ->
                if (db != null) {
                    db.logEntryDao()
                        .filteredCount(
                            likeToken = f.toLikeToken(),
                            levels = f.levels.map { it.name },
                            hasLevelFilter = if (f.levels.isEmpty()) 0 else 1,
                        )
                } else {
                    inMemorySnapshot.map { list -> list.count(f::matches).toLong() }
                }
            }

    public fun entryById(id: String): Flow<LogEntry?> = _database.flatMapLatest { db ->
        if (db != null) {
            db.logEntryDao().selectById(id).map { it?.toDomain() }
        } else {
            inMemorySnapshot.map { list -> list.firstOrNull { it.id == id } }
        }
    }

    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        record(level, tag, message, throwable, metadata = null)
    }

    public fun record(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
        metadata: Map<String, String>? = null,
    ) {
        // Timestamped here, not at write time, so batching never reorders or skews
        // entries. trySend never suspends — a LogWriter must not block its caller.
        pending.trySend(
            LogEntryEntity(
                id = randomUuid(),
                timestamp = currentTimeMillis(),
                level = level.name,
                tag = tag,
                message = message.truncate(),
                throwable = throwable?.stackTraceToString()?.truncate(),
                metadata = metadata?.encodeToJson(),
            )
        )
    }

    private suspend fun writeBatch(batch: List<LogEntryEntity>) {
        val db = _database.value
        if (db != null) {
            db.logEntryDao().insertAll(batch)
            // Once per batch, not once per line — this is the whole point.
            trimDbIfNeeded(db)
        } else if (_inMemory.value != null) {
            // The list is newest-first; the batch arrives oldest-first, so reverse
            // it before prepending.
            val entries = batch.map { it.toDomain() }.asReversed()
            _inMemory.update { list -> (entries + (list ?: emptyList())).take(MAX_ENTRIES.toInt()) }
        }
    }

    /**
     * Snapshot of every entry matching [filter], for export. Bounded by the store's own row cap.
     */
    public suspend fun exportAll(filter: LogFilter): List<LogEntry> {
        val db = _database.value
        return if (db != null) {
            db.logEntryDao()
                .loadPaged(
                    likeToken = filter.toLikeToken(),
                    levels = filter.levels.map { it.name },
                    hasLevelFilter = if (filter.levels.isEmpty()) 0 else 1,
                    limit = MAX_ENTRIES.toInt(),
                    offset = 0,
                )
                .map { it.toDomain() }
        } else {
            inMemorySnapshot.value.filter(filter::matches)
        }
    }

    public suspend fun clear() {
        _database.value?.logEntryDao()?.deleteAll()
        if (_inMemory.value != null) _inMemory.value = emptyList()
    }

    private suspend fun trimDbIfNeeded(db: LogMonitorDatabase) {
        val count = db.logEntryDao().countAll()
        val over = count - MAX_ENTRIES
        if (over > 0) db.logEntryDao().deleteOldestOverLimit(over)
    }

    private fun String.truncate() =
        if (length > MAX_MESSAGE_LENGTH) take(MAX_MESSAGE_LENGTH) + "…" else this

    internal val LogPagingConfig =
        PagingConfig(
            pageSize = 30,
            prefetchDistance = 15,
            initialLoadSize = 60,
            enablePlaceholders = false,
            maxSize = 300,
            jumpThreshold = 120,
        )

    private val StaticLoadStates =
        LoadStates(
            refresh = LoadState.NotLoading(endOfPaginationReached = true),
            prepend = LoadState.NotLoading(endOfPaginationReached = true),
            append = LoadState.NotLoading(endOfPaginationReached = true),
        )
}

internal fun LogEntryEntity.toDomain(): LogEntry =
    LogEntry(
        id = id,
        timestamp = timestamp,
        level =
            when (level) {
                "VERBOSE" -> LogLevel.VERBOSE
                "DEBUG" -> LogLevel.DEBUG
                "INFO" -> LogLevel.INFO
                "WARN" -> LogLevel.WARN
                "ERROR" -> LogLevel.ERROR
                "ASSERT" -> LogLevel.ASSERT
                else -> LogLevel.DEBUG
            },
        tag = tag,
        message = message,
        throwable = throwable,
        metadata = metadata?.decodeToMetadataMap(),
    )

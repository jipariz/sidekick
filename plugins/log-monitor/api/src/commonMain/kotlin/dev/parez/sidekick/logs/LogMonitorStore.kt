package dev.parez.sidekick.logs

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.parez.sidekick.logs.db.LogEntry as DbLogEntry
import dev.parez.sidekick.logs.db.LogMonitorDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

private const val MAX_ENTRIES = 1000L
private const val MAX_MESSAGE_LENGTH = 16_384

object LogMonitorStore : LogCollector {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // SQLDelight-backed storage (android, ios, jvm, js)
    private val _database = MutableStateFlow<LogMonitorDatabase?>(null)

    // In-memory fallback when SQLDelight is unavailable (wasmJs)
    private val _inMemory = MutableStateFlow<List<LogEntry>?>(null)

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

    @OptIn(ExperimentalCoroutinesApi::class)
    val entries: Flow<List<LogEntry>> = _database
        .flatMapLatest { db ->
            if (db != null) {
                db.logEntryQueries
                    .selectAll()
                    .asFlow()
                    .mapToList(Dispatchers.Default)
                    .map { rows -> rows.map { it.toDomain() } }
            } else {
                _inMemory.flatMapLatest { list ->
                    if (list != null) flowOf(list) else emptyFlow()
                }
            }
        }
        .onStart { emit(emptyList()) }

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

    private fun DbLogEntry.toDomain() = LogEntry(
        id = id,
        timestamp = timestamp,
        level = LogLevel.entries.firstOrNull { it.name == level } ?: LogLevel.DEBUG,
        tag = tag,
        message = message,
        throwable = throwable,
        metadata = metadata?.decodeToMetadataMap(),
    )

    private fun String.truncate() =
        if (length > MAX_MESSAGE_LENGTH) take(MAX_MESSAGE_LENGTH) + "…" else this
}


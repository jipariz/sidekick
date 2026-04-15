package dev.parez.sidekick.network

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.parez.sidekick.network.db.NetworkCall as DbNetworkCall
import dev.parez.sidekick.network.db.NetworkMonitorDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

private const val MAX_CALLS = 500L
private const val MAX_BODY_LENGTH = 65_536

object NetworkMonitorStore {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // SQLDelight-backed storage (android, ios, jvm, js)
    private val _database = MutableStateFlow<NetworkMonitorDatabase?>(null)

    // In-memory fallback when SQLDelight is unavailable (wasmJs)
    private val _inMemory = MutableStateFlow<List<NetworkCall>?>(null)

    fun init(retentionPeriod: Duration = RetentionPeriod.ONE_HOUR) {
        scope.launch {
            val driver = createNetworkMonitorDriver()
            if (driver != null) {
                val db = NetworkMonitorDatabase(driver)
                db.networkCallQueries.deleteOlderThan(currentTimeMillis() - retentionPeriod.inWholeMilliseconds)
                _database.value = db
            } else {
                _inMemory.value = emptyList()
            }
        }
    }

    fun calls(): Flow<List<NetworkCall>> = _database
        .flatMapLatest { db ->
            if (db != null) {
                db.networkCallQueries
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

    suspend fun recordRequest(
        id: String,
        url: String,
        method: String,
        headers: Map<String, String>,
        body: String?,
        timestamp: Long,
    ) {
        val db = _database.value
        if (db != null) {
            db.networkCallQueries.insertCall(
                id = id,
                url = url,
                method = method,
                requestHeaders = headers.encodeToJson(),
                requestBody = body?.truncate(),
                requestTimestamp = timestamp,
            )
            trimDbIfNeeded(db)
        } else if (_inMemory.value != null) {
            val call = NetworkCall(
                id = id, url = url, method = method,
                requestHeaders = headers, requestBody = body?.truncate(),
                requestTimestamp = timestamp, responseCode = null,
                responseHeaders = emptyMap(), responseBody = null,
                responseTimestamp = null, error = null, status = CallStatus.PENDING,
            )
            _inMemory.update { list -> (listOf(call) + (list ?: emptyList())).take(MAX_CALLS.toInt()) }
        }
    }

    suspend fun recordResponse(
        id: String,
        code: Int,
        headers: Map<String, String>,
        timestamp: Long,
    ) {
        val db = _database.value
        if (db != null) {
            db.networkCallQueries.updateResponse(
                responseCode = code.toLong(),
                responseHeaders = headers.encodeToJson(),
                responseBody = null,
                responseTimestamp = timestamp,
                id = id,
            )
        } else if (_inMemory.value != null) {
            _inMemory.update { list ->
                list?.map { call ->
                    if (call.id == id) call.copy(
                        responseCode = code, responseHeaders = headers,
                        responseTimestamp = timestamp, status = CallStatus.COMPLETE,
                    ) else call
                }
            }
        }
    }

    suspend fun recordResponseBody(id: String, body: String) {
        val db = _database.value
        if (db != null) {
            db.networkCallQueries.updateResponseBody(responseBody = body.truncate(), id = id)
        } else if (_inMemory.value != null) {
            _inMemory.update { list ->
                list?.map { call ->
                    if (call.id == id) call.copy(responseBody = body.truncate()) else call
                }
            }
        }
    }

    suspend fun recordError(id: String, error: Throwable) {
        val db = _database.value
        if (db != null) {
            db.networkCallQueries.updateError(error = error.message ?: error.toString(), id = id)
        } else if (_inMemory.value != null) {
            _inMemory.update { list ->
                list?.map { call ->
                    if (call.id == id) call.copy(
                        error = error.message ?: error.toString(),
                        status = CallStatus.ERROR,
                    ) else call
                }
            }
        }
    }

    suspend fun clear() {
        _database.value?.networkCallQueries?.deleteAll()
        if (_inMemory.value != null) _inMemory.value = emptyList()
    }

    private suspend fun trimDbIfNeeded(db: NetworkMonitorDatabase) {
        val count = db.networkCallQueries.countAll().awaitAsOne()
        val over = count - MAX_CALLS
        if (over > 0) db.networkCallQueries.deleteOldestOverLimit(over)
    }

    private fun DbNetworkCall.toDomain() = NetworkCall(
        id = id, url = url, method = method,
        requestHeaders = requestHeaders.decodeToHeaderMap(),
        requestBody = requestBody, requestTimestamp = requestTimestamp,
        responseCode = responseCode?.toInt(),
        responseHeaders = responseHeaders.decodeToHeaderMap(),
        responseBody = responseBody, responseTimestamp = responseTimestamp,
        error = error,
        status = when (status) {
            "COMPLETE" -> CallStatus.COMPLETE
            "ERROR"    -> CallStatus.ERROR
            else       -> CallStatus.PENDING
        },
    )

    private fun String.truncate() = if (length > MAX_BODY_LENGTH) take(MAX_BODY_LENGTH) + "…" else this
}

object RetentionPeriod {
    val ONE_HOUR: Duration = 1.hours
    val ONE_DAY: Duration  = 24.hours
    val ONE_WEEK: Duration = 7.days
    val FOREVER: Duration  = Duration.INFINITE
}

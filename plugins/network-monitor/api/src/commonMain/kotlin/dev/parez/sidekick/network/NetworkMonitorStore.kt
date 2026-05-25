package dev.parez.sidekick.network

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.parez.sidekick.network.db.NetworkMonitorDatabase
import dev.parez.sidekick.network.paging.NetworkCallPagingSource
import dev.parez.sidekick.network.paging.toDomain
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

private const val MAX_CALLS = 500L
private const val MAX_BODY_LENGTH = 65_536

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkMonitorStore(private val scope: CoroutineScope) {

    // SQLDelight-backed storage (android, ios, jvm, js)
    private val _database = MutableStateFlow<NetworkMonitorDatabase?>(null)

    // In-memory fallback when SQLDelight is unavailable (wasmJs)
    private val _inMemory = MutableStateFlow<List<NetworkCall>?>(null)

    // Hot StateFlow view of the in-memory list, used as the source for
    // InMemoryNetworkCallPagingSource.
    // Owned by the store's scope so it outlives ViewModel-scoped pagers.
    private val inMemorySnapshot: StateFlow<List<NetworkCall>> =
        _inMemory.filterNotNull().stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val initialized = MutableStateFlow(false)

    fun init(retentionPeriod: Duration = 1.hours) {
        if (!initialized.compareAndSet(expect = false, update = true)) return

        scope.launch {
            val driver = createNetworkMonitorDriver()
            if (driver != null) {
                val db = NetworkMonitorDatabase(driver)
                db.networkCallQueries.deleteOlderThan(
                    currentTimeMillis() - retentionPeriod.inWholeMilliseconds
                )
                _database.value = db
            } else {
                _inMemory.value = emptyList()
            }
        }
    }

    fun pagedCalls(filter: Flow<NetworkFilter>): Flow<PagingData<NetworkCall>> =
        combine(_database, filter.distinctUntilChanged()) { db, f -> db to f }
            .flatMapLatest { (db, f) ->
                if (db != null) {
                    Pager(
                            config = NetworkPagingConfig,
                            pagingSourceFactory = { NetworkCallPagingSource(db, f) },
                        )
                        .flow
                } else {
                    // In-memory fallback (wasmJs): the list is capped at MAX_CALLS
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

    fun filteredCount(filter: Flow<NetworkFilter>): Flow<Long> =
        combine(_database, filter.distinctUntilChanged()) { db, f -> db to f }
            .flatMapLatest { (db, f) ->
                if (db != null) {
                    val token = f.toLikeToken()
                    if (f.methods.isEmpty()) {
                        db.networkCallQueries
                            .countFilteredAllMethods(token)
                            .asFlow()
                            .mapToOne(Dispatchers.Default)
                    } else {
                        db.networkCallQueries
                            .countFiltered(token, f.methods)
                            .asFlow()
                            .mapToOne(Dispatchers.Default)
                    }
                } else {
                    inMemorySnapshot.map { list -> list.count(f::matches).toLong() }
                }
            }

    fun callById(id: String): Flow<NetworkCall?> = _database.flatMapLatest { db ->
        if (db != null) {
            db.networkCallQueries.selectById(id).asFlow().mapToOneOrNull(Dispatchers.Default).map {
                it?.toDomain()
            }
        } else {
            inMemorySnapshot.map { list -> list.firstOrNull { it.id == id } }
        }
    }

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
            val call =
                NetworkCall(
                    id = id,
                    url = url,
                    method = method,
                    requestHeaders = headers,
                    requestBody = body?.truncate(),
                    requestTimestamp = timestamp,
                    responseCode = null,
                    responseHeaders = emptyMap(),
                    responseBody = null,
                    responseTimestamp = null,
                    error = null,
                    status = CallStatus.PENDING,
                )
            _inMemory.update { list ->
                (listOf(call) + (list ?: emptyList())).take(MAX_CALLS.toInt())
            }
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
                    if (call.id == id)
                        call.copy(
                            responseCode = code,
                            responseHeaders = headers,
                            responseTimestamp = timestamp,
                            status = CallStatus.COMPLETE,
                        )
                    else call
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
                    if (call.id == id)
                        call.copy(
                            error = error.message ?: error.toString(),
                            status = CallStatus.ERROR,
                        )
                    else call
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

    private fun String.truncate() =
        if (length > MAX_BODY_LENGTH) take(MAX_BODY_LENGTH) + "…" else this

    internal companion object {
        internal val NetworkPagingConfig =
            PagingConfig(
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
        private val StaticLoadStates =
            LoadStates(
                refresh = LoadState.NotLoading(endOfPaginationReached = true),
                prepend = LoadState.NotLoading(endOfPaginationReached = true),
                append = LoadState.NotLoading(endOfPaginationReached = true),
            )
    }
}

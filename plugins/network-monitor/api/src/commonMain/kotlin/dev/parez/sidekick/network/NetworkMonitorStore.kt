package dev.parez.sidekick.network

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import dev.parez.sidekick.network.db.NetworkCallEntity
import dev.parez.sidekick.network.db.NetworkMonitorDatabase
import dev.parez.sidekick.network.db.createNetworkMonitorDatabase
import dev.parez.sidekick.network.paging.NetworkCallPagingSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
public class NetworkMonitorStore(private val scope: CoroutineScope) {

    // Room-backed storage (android, ios, jvm). Web targets fall through to the
    // in-memory list below — see createNetworkMonitorDatabase.{js,wasmJs}.kt.
    private val _database = MutableStateFlow<NetworkMonitorDatabase?>(null)

    private val _inMemory = MutableStateFlow<List<NetworkCall>?>(null)

    // Hot StateFlow view of the in-memory list, used as the source for the
    // web fallback PagingData. Owned by the store's scope so it outlives
    // ViewModel-scoped pagers.
    private val inMemorySnapshot: StateFlow<List<NetworkCall>> =
        _inMemory.filterNotNull().stateIn(scope, SharingStarted.Eagerly, emptyList())

    /**
     * Entries ever accepted, never decreasing.
     *
     * The unread badge cannot be derived from the retained row count: once the store is at its cap
     * — the normal steady state — every new entry evicts an old one and the count stops moving, so
     * a count-based badge silently stops reporting new activity.
     */
    private val _recordedCount = MutableStateFlow(0L)
    public val recordedCount: StateFlow<Long> = _recordedCount.asStateFlow()

    private val initialized = MutableStateFlow(false)

    // Aggregate body ceiling, applied on both the Room and in-memory paths.
    private var bodyBudgetChars: Int = BodyBudget.Default

    public fun init(
        retentionPeriod: Duration = 1.hours,
        bodyBudgetChars: Int = BodyBudget.Default,
    ) {
        if (!initialized.compareAndSet(expect = false, update = true)) return
        this.bodyBudgetChars = bodyBudgetChars

        scope.launch {
            val db = createNetworkMonitorDatabase()
            val ready =
                db != null &&
                    runCatching {
                            // Probes the underlying driver. On web targets the
                            // WebWorkerSQLiteDriver only fails on first SQL use, so
                            // a no-op DAO call surfaces a missing sqlite-web worker.
                            db.networkCallDao()
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
        }
    }

    public fun pagedCalls(filter: Flow<NetworkFilter>): Flow<PagingData<NetworkCall>> =
        combine(_database, filter.distinctUntilChanged()) { db, f -> db to f }
            .flatMapLatest { (db, f) ->
                if (db != null) {
                    Pager(
                            config = NetworkPagingConfig,
                            pagingSourceFactory = { NetworkCallPagingSource(db, f, scope) },
                        )
                        .flow
                } else {
                    // In-memory fallback (js, wasmJs): the list is capped at MAX_CALLS
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

    public fun filteredCount(filter: Flow<NetworkFilter>): Flow<Long> =
        combine(_database, filter.distinctUntilChanged()) { db, f -> db to f }
            .flatMapLatest { (db, f) ->
                if (db != null) {
                    db.networkCallDao()
                        .filteredCount(
                            likeToken = f.toLikeToken(),
                            methods = f.methods.toList(),
                            hasMethodFilter = if (f.methods.isEmpty()) 0 else 1,
                        )
                } else {
                    inMemorySnapshot.map { list -> list.count(f::matches).toLong() }
                }
            }

    public fun callById(id: String): Flow<NetworkCall?> = _database.flatMapLatest { db ->
        if (db != null) {
            db.networkCallDao().selectById(id).map { it?.toDomain() }
        } else {
            inMemorySnapshot.map { list -> list.firstOrNull { it.id == id } }
        }
    }

    public suspend fun recordRequest(
        id: String,
        url: String,
        method: String,
        headers: Map<String, String>,
        body: String?,
        timestamp: Long,
    ) {
        _recordedCount.update { it + 1 }
        val db = _database.value
        if (db != null) {
            db.networkCallDao()
                .insert(
                    NetworkCallEntity(
                        id = id,
                        url = url,
                        method = method,
                        requestHeaders = headers.encodeToJson(),
                        requestBody = body?.truncate(),
                        requestTimestamp = timestamp,
                        responseCode = null,
                        responseHeaders = "{}",
                        responseBody = null,
                        responseTimestamp = null,
                        error = null,
                        status = "PENDING",
                    )
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
                (listOf(call) + (list ?: emptyList())).take(MAX_CALLS.toInt()).enforceBodyBudget()
            }
        }
    }

    public suspend fun recordResponse(
        id: String,
        code: Int,
        headers: Map<String, String>,
        timestamp: Long,
    ) {
        val db = _database.value
        if (db != null) {
            db.networkCallDao()
                .updateResponse(
                    id = id,
                    code = code,
                    headers = headers.encodeToJson(),
                    body = null,
                    timestamp = timestamp,
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

    public suspend fun recordResponseBody(id: String, body: String) {
        val db = _database.value
        if (db != null) {
            db.networkCallDao().updateResponseBody(id = id, body = body.truncate())
            // Response bodies are the large ones and they land *after* their request,
            // so leaving enforcement to recordRequest let a burst of big responses sit
            // over budget indefinitely whenever traffic stopped.
            evictBodiesOverBudget(db)
        } else if (_inMemory.value != null) {
            _inMemory.update { list ->
                list
                    ?.map { call ->
                        if (call.id == id) call.copy(responseBody = body.truncate()) else call
                    }
                    ?.enforceBodyBudget()
            }
        }
    }

    public suspend fun recordError(id: String, error: Throwable) {
        val db = _database.value
        if (db != null) {
            db.networkCallDao().updateError(id = id, error = error.message ?: error.toString())
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

    /**
     * Snapshot of every call matching [filter], for export. Bounded by the store's own row cap, so
     * "all" is always a finite list — this reads the same rows the list screen pages through, not a
     * separate unbounded query.
     */
    public suspend fun exportAll(filter: NetworkFilter): List<NetworkCall> {
        val db = _database.value
        return if (db != null) {
            db.networkCallDao()
                .loadPaged(
                    likeToken = filter.toLikeToken(),
                    methods = filter.methods.toList(),
                    hasMethodFilter = if (filter.methods.isEmpty()) 0 else 1,
                    limit = MAX_CALLS.toInt(),
                    offset = 0,
                )
                .map { it.toDomain() }
        } else {
            inMemorySnapshot.value.filter(filter::matches)
        }
    }

    public suspend fun clear() {
        _database.value?.networkCallDao()?.deleteAll()
        if (_inMemory.value != null) _inMemory.value = emptyList()
    }

    private suspend fun trimDbIfNeeded(db: NetworkMonitorDatabase) {
        val count = db.networkCallDao().countAll()
        val over = count - MAX_CALLS
        if (over > 0) db.networkCallDao().deleteOldestOverLimit(over)
        evictBodiesOverBudget(db)
    }

    private suspend fun evictBodiesOverBudget(db: NetworkMonitorDatabase) {
        if (bodyBudgetChars == BodyBudget.Unlimited) return
        db.networkCallDao().evictBodiesOverBudget(bodyBudgetChars.toLong())
    }

    /**
     * In-memory counterpart of [NetworkCallDao.evictBodiesOverBudget]. Walks newest -> oldest
     * accumulating body length and drops both bodies once the running total passes the budget. This
     * is the web fallback path, where the list lives in the JS heap.
     */
    private fun List<NetworkCall>.enforceBodyBudget(): List<NetworkCall> {
        if (bodyBudgetChars == BodyBudget.Unlimited) return this
        var running = 0L
        var evicting = false
        return map { call ->
            if (evicting) {
                if (call.requestBody == null && call.responseBody == null) call
                else call.copy(requestBody = null, responseBody = null, bodiesEvicted = true)
            } else {
                running += (call.requestBody?.length ?: 0) + (call.responseBody?.length ?: 0)
                if (running > bodyBudgetChars) {
                    evicting = true
                    call.copy(requestBody = null, responseBody = null, bodiesEvicted = true)
                } else {
                    call
                }
            }
        }
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

internal fun NetworkCallEntity.toDomain() =
    NetworkCall(
        id = id,
        url = url,
        method = method,
        requestHeaders = requestHeaders.decodeToHeaderMap(),
        requestBody = requestBody,
        requestTimestamp = requestTimestamp,
        responseCode = responseCode,
        responseHeaders = responseHeaders.decodeToHeaderMap(),
        responseBody = responseBody,
        responseTimestamp = responseTimestamp,
        error = error,
        bodiesEvicted = bodiesEvicted,
        status =
            when (status) {
                "COMPLETE" -> CallStatus.COMPLETE
                "ERROR" -> CallStatus.ERROR
                else -> CallStatus.PENDING
            },
    )

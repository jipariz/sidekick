package dev.parez.sidekick.network.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface NetworkCallDao {

    // `:hasMethodFilter` is a flag that lets a single query cover both the
    // "all methods" and "method IN (...)" cases. Room rejects `IN ()` (empty
    // list) at runtime, so the SQLDelight schema used to ship two queries —
    // this collapses them.
    //
    // The paged read returns a List (not a PagingSource) because Room 3 KMP's
    // PagingSource adapter is Android-only — non-Android source sets only
    // permit suspend/Flow returns. NetworkCallPagingSource wraps this with
    // offset-based loading + InvalidationTracker for change notifications.
    @Query(
        """
        SELECT * FROM network_calls
        WHERE (url LIKE :likeToken OR method LIKE :likeToken)
          AND (:hasMethodFilter = 0 OR method IN (:methods))
        ORDER BY requestTimestamp DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun loadPaged(
        likeToken: String,
        methods: List<String>,
        hasMethodFilter: Int,
        limit: Int,
        offset: Int,
    ): List<NetworkCallEntity>

    @Query(
        """
        SELECT COUNT(*) FROM network_calls
        WHERE (url LIKE :likeToken OR method LIKE :likeToken)
          AND (:hasMethodFilter = 0 OR method IN (:methods))
        """
    )
    fun filteredCount(likeToken: String, methods: List<String>, hasMethodFilter: Int): Flow<Long>

    @Query("SELECT * FROM network_calls WHERE id = :id")
    fun selectById(id: String): Flow<NetworkCallEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(entity: NetworkCallEntity)

    @Query(
        """
        UPDATE network_calls
        SET responseCode = :code, responseHeaders = :headers,
            responseBody = :body, responseTimestamp = :timestamp,
            status = 'COMPLETE'
        WHERE id = :id
        """
    )
    suspend fun updateResponse(
        id: String,
        code: Int,
        headers: String,
        body: String?,
        timestamp: Long,
    )

    @Query("UPDATE network_calls SET responseBody = :body WHERE id = :id")
    suspend fun updateResponseBody(id: String, body: String)

    @Query("UPDATE network_calls SET error = :error, status = 'ERROR' WHERE id = :id")
    suspend fun updateError(id: String, error: String)

    @Query("DELETE FROM network_calls") suspend fun deleteAll()

    @Query("DELETE FROM network_calls WHERE requestTimestamp < :threshold")
    suspend fun deleteOlderThan(threshold: Long)

    @Query("SELECT COUNT(*) FROM network_calls") suspend fun countAll(): Long

    @Query(
        """
        DELETE FROM network_calls WHERE id IN (
            SELECT id FROM network_calls ORDER BY requestTimestamp ASC LIMIT :limit
        )
        """
    )
    suspend fun deleteOldestOverLimit(limit: Long)
}

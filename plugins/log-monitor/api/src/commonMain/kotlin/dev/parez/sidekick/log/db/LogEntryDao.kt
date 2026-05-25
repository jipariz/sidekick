package dev.parez.sidekick.log.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface LogEntryDao {

    // See the matching note in NetworkCallDao — non-Android targets only
    // allow suspend/Flow returns, so the paged read is List-shaped and
    // LogEntryPagingSource handles offset paging + invalidation.
    @Query(
        """
        SELECT * FROM log_entries
        WHERE (tag LIKE :likeToken OR message LIKE :likeToken)
          AND (:hasLevelFilter = 0 OR level IN (:levels))
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun loadPaged(
        likeToken: String,
        levels: List<String>,
        hasLevelFilter: Int,
        limit: Int,
        offset: Int,
    ): List<LogEntryEntity>

    @Query(
        """
        SELECT COUNT(*) FROM log_entries
        WHERE (tag LIKE :likeToken OR message LIKE :likeToken)
          AND (:hasLevelFilter = 0 OR level IN (:levels))
        """
    )
    fun filteredCount(likeToken: String, levels: List<String>, hasLevelFilter: Int): Flow<Long>

    @Query("SELECT * FROM log_entries WHERE id = :id")
    fun selectById(id: String): Flow<LogEntryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(entity: LogEntryEntity)

    @Query("DELETE FROM log_entries") suspend fun deleteAll()

    @Query("DELETE FROM log_entries WHERE timestamp < :threshold")
    suspend fun deleteOlderThan(threshold: Long)

    @Query("SELECT COUNT(*) FROM log_entries") suspend fun countAll(): Long

    @Query(
        """
        DELETE FROM log_entries WHERE id IN (
            SELECT id FROM log_entries ORDER BY timestamp ASC LIMIT :limit
        )
        """
    )
    suspend fun deleteOldestOverLimit(limit: Long)
}

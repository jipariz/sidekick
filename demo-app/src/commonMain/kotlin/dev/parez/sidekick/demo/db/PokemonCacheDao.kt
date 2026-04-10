package dev.parez.sidekick.demo.db

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert

@Dao
interface PokemonCacheDao {

    @Query("SELECT * FROM cached_list_page WHERE offset = :offset")
    suspend fun getListPage(offset: Int): CachedListPageEntity?

    @Upsert
    suspend fun upsertListPage(page: CachedListPageEntity)

    @Query("SELECT * FROM cached_detail WHERE id = :id")
    suspend fun getDetail(id: Int): CachedDetailEntity?

    @Upsert
    suspend fun upsertDetail(detail: CachedDetailEntity)
}

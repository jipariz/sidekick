package dev.parez.sidekick.demo.db

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "cached_list_page")
data class CachedListPageEntity(
    @PrimaryKey val offset: Int,
    val responseJson: String,
)

@Entity(tableName = "cached_detail")
data class CachedDetailEntity(
    @PrimaryKey val id: Int,
    val responseJson: String,
)

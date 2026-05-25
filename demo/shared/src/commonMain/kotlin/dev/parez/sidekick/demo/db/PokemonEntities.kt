package dev.parez.sidekick.demo.db

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Catalog row — the cheap `/pokemon` list endpoint result.
 *
 * Split out from the full [PokemonDetailEntity] so the table has zero nullable columns. The web
 * SQLite worker (`androidx.sqlite:sqlite-web:2.7.0-alpha05`) caches column types per statement, not
 * per row, and reads NULL cells as the cached non-null type — which trips Room's `getLong` on any
 * nullable Int column. Keeping every column NOT NULL sidesteps that bug entirely.
 */
@Entity(tableName = "pokemon_list")
data class PokemonListEntity(@PrimaryKey val id: Int, val name: String)

/**
 * Detail row — populated only after `/pokemon/{id}` succeeds. Absence of a row for a given id
 * signals "detail not yet fetched"; `observeDetail` returns `Flow<PokemonDetailEntity?>` so Room
 * emits `null` cleanly without ever reading column values.
 */
@Entity(tableName = "pokemon_detail")
data class PokemonDetailEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val height: Int,
    val weight: Int,
    val typesJson: String,
    val statsJson: String,
    val abilitiesJson: String,
)

package dev.parez.sidekick.demo.db

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "pokemon")
data class PokemonEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val height: Int? = null,
    val weight: Int? = null,
    val typesJson: String? = null,
    val statsJson: String? = null,
    val abilitiesJson: String? = null,
)

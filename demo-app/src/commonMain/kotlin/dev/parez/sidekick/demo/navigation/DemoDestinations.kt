package dev.parez.sidekick.demo.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object PokemonListDestination : NavKey

@Serializable
data class PokemonDetailDestination(val id: Int, val name: String) : NavKey

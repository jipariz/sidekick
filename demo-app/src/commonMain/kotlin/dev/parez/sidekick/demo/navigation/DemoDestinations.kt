package dev.parez.sidekick.demo.navigation

/**
 * Represents the Pokémon list screen (the "list" pane).
 */
data object PokemonListDestination

/**
 * Represents the Pokémon detail screen (the "detail" pane).
 */
data class PokemonDetailDestination(val id: Int, val name: String)

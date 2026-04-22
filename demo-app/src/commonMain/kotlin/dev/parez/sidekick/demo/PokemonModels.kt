package dev.parez.sidekick.demo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── List endpoint ─────────────────────────────────────────────────────────────

@Serializable
data class PokemonListResponse(
    val count: Int,
    val next: String?,
    val results: List<PokemonListEntry>,
)

@Serializable
data class PokemonListEntry(
    val name: String,
    val url: String,
) {
    /** Extracted from the URL: "https://pokeapi.co/api/v2/pokemon/1/" → 1 */
    val id: Int get() = url.trimEnd('/').substringAfterLast('/').toInt()
    val spriteUrl: String get() = spriteUrlFor(id)
}

// ── Detail endpoint ───────────────────────────────────────────────────────────

@Serializable
data class PokemonDetail(
    val id: Int,
    val name: String,
    val height: Int,     // decimetres
    val weight: Int,     // hectograms
    val types: List<TypeSlot>,
    val stats: List<StatEntry>,
    val abilities: List<AbilitySlot>,
)

@Serializable
data class TypeSlot(
    val slot: Int,
    val type: NamedResource,
)

@Serializable
data class StatEntry(
    @SerialName("base_stat") val baseStat: Int,
    val stat: NamedResource,
)

@Serializable
data class AbilitySlot(
    val ability: NamedResource,
    @SerialName("is_hidden") val isHidden: Boolean,
)

@Serializable
data class NamedResource(val name: String)

// ── URL helpers ───────────────────────────────────────────────────────────────

fun spriteUrlFor(id: Int, shiny: Boolean = false): String {
    val path = if (shiny) "pokemon/shiny/$id.png" else "pokemon/$id.png"
    return "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/$path"
}

fun artworkUrlFor(id: Int, shiny: Boolean = false): String {
    val path = if (shiny) "official-artwork/shiny/$id.png" else "official-artwork/$id.png"
    return "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/$path"
}

// ── Display helpers ───────────────────────────────────────────────────────────

fun String.toDisplayName(): String = replace('-', ' ')
    .split(' ')
    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

fun statDisplayName(apiName: String): String = when (apiName) {
    "hp"              -> "HP"
    "attack"          -> "Attack"
    "defense"         -> "Defense"
    "special-attack"  -> "Sp.Atk"
    "special-defense" -> "Sp.Def"
    "speed"           -> "Speed"
    else              -> apiName.toDisplayName()
}

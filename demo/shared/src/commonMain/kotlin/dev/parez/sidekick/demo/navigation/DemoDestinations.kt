package dev.parez.sidekick.demo.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** The list pane — the catalog of Pokémon. */
@Serializable
data object PokemonListKey : NavKey

/** The detail pane — a single Pokémon. */
@Serializable
data class PokemonDetailKey(val id: Int, val name: String) : NavKey

/** The Sidekick debug overlay — a full-screen destination above the catalog. */
@Serializable
data object SidekickKey : NavKey

/**
 * Required by `rememberNavBackStack` on non-Android targets: registers each
 * concrete [NavKey] subtype so the polymorphic serializer can round-trip the
 * backstack across process death.
 */
val DemoSavedStateConfiguration: SavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(PokemonListKey::class, PokemonListKey.serializer())
            subclass(PokemonDetailKey::class, PokemonDetailKey.serializer())
            subclass(SidekickKey::class, SidekickKey.serializer())
        }
    }
}

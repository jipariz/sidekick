package dev.parez.sidekick.demo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.github.terrakok.navigation3.browser.ChronologicalBrowserNavigation
import com.github.terrakok.navigation3.browser.buildBrowserHistoryFragment
import com.github.terrakok.navigation3.browser.getBrowserHistoryFragmentName
import com.github.terrakok.navigation3.browser.getBrowserHistoryFragmentParameters

/**
 * Web actual — binds the nav3 [backStack] to the browser History via terrakok's
 * `navigation3-browser` chronological binding.
 *
 * - [PokemonListKey] → `#home`
 * - [PokemonDetailKey] → `#pokemon?id=…&name=…`
 *
 * The lib observes the backstack and writes to history; it also rewrites the backstack on browser
 * back/forward and on first-load deep links. We're using the [NavBackStack]-typed overload (not the
 * generic `SnapshotStateList<T>` one), so the keys we hand it are real [NavKey] instances and
 * `restoreKey` is allowed to round-trip every member of the stack.
 *
 * Note: `restoreKey` must return non-null for any key that should be restorable on browser
 * back/forward — a `null` return is the lib's parse-failure sentinel and aborts the popstate
 * restore.
 */
@Composable
actual fun BrowserHistoryEffect(backStack: NavBackStack<NavKey>) {
    ChronologicalBrowserNavigation(
        backStack = backStack,
        saveKey = { key ->
            when (key) {
                is PokemonListKey -> buildBrowserHistoryFragment("home")
                is PokemonDetailKey ->
                    buildBrowserHistoryFragment(
                        name = "pokemon",
                        parameters = mapOf("id" to key.id.toString(), "name" to key.name),
                    )
                is SidekickKey -> buildBrowserHistoryFragment("sidekick")
                else -> null
            }
        },
        restoreKey = { fragment ->
            when (getBrowserHistoryFragmentName(fragment)) {
                "home" -> PokemonListKey
                "pokemon" -> {
                    val params = getBrowserHistoryFragmentParameters(fragment)
                    val id = params["id"]?.toIntOrNull()
                    val pokemonName = params["name"].orEmpty()
                    if (id != null) PokemonDetailKey(id, pokemonName) else null
                }
                "sidekick" -> SidekickKey
                // Empty hash on first load — treat as the list root.
                null -> if (fragment.isBlank()) PokemonListKey else null
                else -> null
            }
        },
    )
}

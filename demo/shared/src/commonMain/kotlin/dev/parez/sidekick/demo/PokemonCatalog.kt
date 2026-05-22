package dev.parez.sidekick.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CatchingPokemon
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import dev.parez.sidekick.Sidekick
import dev.parez.sidekick.demo.navigation.PokemonDetailKey
import dev.parez.sidekick.demo.navigation.PokemonListKey
import dev.parez.sidekick.demo.navigation.SidekickKey
import dev.parez.sidekick.demo.ui.PokemonDetailScreen
import dev.parez.sidekick.demo.ui.PokemonListScreen
import dev.parez.sidekick.plugin.SidekickPlugin

/**
 * Adaptive list-detail catalog driven by AndroidX Navigation 3.
 *
 * [backStack] is the single source of truth — pushing a [PokemonDetailKey]
 * opens the detail pane, popping returns to the list. On wide windows the
 * [ListDetailSceneStrategy] keeps both panes visible; on narrow windows it
 * collapses to one. On web, the same backstack is bound to browser history via
 * [dev.parez.sidekick.demo.navigation.BrowserHistoryEffect].
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun PokemonCatalog(
    backStack: NavBackStack<NavKey>,
    showNumbers: Boolean,
    shinySprites: Boolean,
    plugins: List<SidekickPlugin>,
) {
    // Drop the default horizontal gutter between the two panes — same tweak as
    // the official Material recipe (b/418201867).
    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo)
            .copy(horizontalPartitionSpacerSize = 0.dp)
    }
    val sceneStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        sceneStrategies = listOf(sceneStrategy),
        entryProvider = entryProvider {
            entry<PokemonListKey>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { DetailPlaceholder() },
                ),
            ) {
                PokemonListScreen(
                    showNumbers = showNumbers,
                    shinySprites = shinySprites,
                    onSelect = { entry ->
                        val key = PokemonDetailKey(entry.id, entry.name)
                        // In two-pane mode the list stays visible alongside the
                        // detail, so picking another Pokémon should swap the
                        // detail in place rather than stack on top — that way
                        // back always returns to the list, not to the previous
                        // detail.
                        if (backStack.lastOrNull() is PokemonDetailKey) {
                            backStack[backStack.lastIndex] = key
                        } else {
                            backStack.add(key)
                        }
                    },
                )
            }
            entry<PokemonDetailKey>(
                metadata = ListDetailSceneStrategy.detailPane(),
            ) { key ->
                PokemonDetailScreen(
                    id = key.id,
                    name = key.name,
                    onBack = { backStack.removeLastOrNull() },
                    shinySprites = shinySprites,
                )
            }
            // No list/detail metadata — falls through to the default single-pane
            // scene, so Sidekick renders full-bleed on top of whichever pane
            // was previously active.
            entry<SidekickKey> {
                Sidekick(
                    useSidekickTheme = false,
                    plugins = plugins,
                    actions = {
                        IconButton(
                            onClick = { backStack.removeLastOrNull() },
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close Sidekick")
                        }
                    },
                )
            }
        },
    )
}

@Composable
private fun DetailPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.CatchingPokemon,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Select a Pokémon",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

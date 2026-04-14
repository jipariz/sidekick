package dev.parez.sidekick.demo

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import dev.parez.sidekick.SidekickShell
import dev.parez.sidekick.demo.di.appModule
import dev.parez.sidekick.demo.navigation.PokemonDetailDestination
import dev.parez.sidekick.demo.navigation.PokemonListDestination
import dev.parez.sidekick.demo.theme.AppTypography
import dev.parez.sidekick.demo.theme.colorSchemeFor
import dev.parez.sidekick.demo.ui.PokemonDetailScreen
import dev.parez.sidekick.demo.ui.PokemonListScreen
import dev.parez.sidekick.network.NetworkMonitorPlugin
import dev.parez.sidekick.network.RetentionPeriod
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.KoinApplication

// ── Navigation serializer module ─────────────────────────────────────────────

private val navSerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(PokemonListDestination::class)
        subclass(PokemonDetailDestination::class)
    }
}

private val navSavedStateConfig = SavedStateConfiguration {
    serializersModule = navSerializersModule
}

// ── Root composable ───────────────────────────────────────────────────────────

@Composable
fun DemoApp() {
    KoinApplication(application = { modules(appModule) }) {
        val prefsPlugin = remember { AppPreferencesPlugin() }
        val networkPlugin = remember { NetworkMonitorPlugin(retentionMs = RetentionPeriod.ONE_HOUR) }

        val darkMode by prefsPlugin.accessor.darkMode.collectAsState()
        val colorTheme by prefsPlugin.accessor.colorTheme.collectAsState()
        val showNumbers by prefsPlugin.accessor.showNumbers.collectAsState()
        val gridColumns by prefsPlugin.accessor.gridColumns.collectAsState()

        val colorScheme = colorSchemeFor(theme = colorTheme, dark = darkMode)

        MaterialTheme(colorScheme = colorScheme, typography = AppTypography) {
            SidekickShell(plugins = listOf(prefsPlugin, networkPlugin)) {
                PokemonCatalog(
                    showNumbers = showNumbers,
                    gridColumns = gridColumns,
                )
            }
        }
    }
}

// ── Catalog (adaptive list-detail with Navigation 3) ─────────────────────────

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun PokemonCatalog(
    showNumbers: Boolean,
    gridColumns: Int,
) {
    val backStack = rememberNavBackStack(navSavedStateConfig, PokemonListDestination)
    val sceneStrategy = rememberListDetailSceneStrategy<NavKey>()

    NavDisplay(
        backStack = backStack,
        sceneStrategies = listOf(sceneStrategy),
        entryProvider = entryProvider {
            entry<PokemonListDestination>(
                metadata = ListDetailSceneStrategy.listPane(),
            ) {
                PokemonListScreen(
                    columns = gridColumns,
                    showNumbers = showNumbers,
                    onSelect = { entry ->
                        backStack += PokemonDetailDestination(entry.id, entry.name)
                    },
                )
            }

            entry<PokemonDetailDestination>(
                metadata = ListDetailSceneStrategy.detailPane(),
            ) { destination ->
                PokemonDetailScreen(
                    id = destination.id,
                    name = destination.name,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}

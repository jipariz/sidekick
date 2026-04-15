package dev.parez.sidekick.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.CatchingPokemon
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import dev.parez.sidekick.logs.LogMonitorPlugin
import dev.parez.sidekick.logs.RetentionPeriod as LogRetentionPeriod
import dev.parez.sidekick.screens.CustomScreenPlugin
import dev.parez.sidekick.logs.kermit.LogMonitorLogWriter
import dev.parez.sidekick.network.NetworkMonitorPlugin
import dev.parez.sidekick.network.RetentionPeriod
import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter
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
        val networkPlugin = remember { NetworkMonitorPlugin(retentionPeriod = RetentionPeriod.ONE_HOUR) }
        val logPlugin = remember {
            LogMonitorPlugin(retentionPeriod = LogRetentionPeriod.ONE_HOUR).also { plugin ->
                Logger.setLogWriters(platformLogWriter(), LogMonitorLogWriter(plugin.store))
            }
        }

        val darkMode by prefsPlugin.accessor.darkMode.collectAsState()
        val colorTheme by prefsPlugin.accessor.colorTheme.collectAsState()
        val showNumbers by prefsPlugin.accessor.showNumbers.collectAsState()
        val gridColumns by prefsPlugin.accessor.gridColumns.collectAsState()

        val colorScheme = colorSchemeFor(theme = colorTheme, dark = darkMode)

        // Example: two custom screens added to the overlay via CustomScreenPlugin.
        // DI works naturally inside the content lambdas (they run in the host
        // app's composition tree, so Koin / Hilt / CompositionLocals are all
        // available without any extra wiring).
        val buildInfoPlugin = remember {
            CustomScreenPlugin(
                id = "build-info",
                title = "Build Info",
                icon = Icons.Default.Info,
                content = { BuildInfoScreen() },
            )
        }
        val demoScreen2Plugin = remember {
            CustomScreenPlugin(
                id = "custom-debug",
                title = "Custom Debug",
                icon = Icons.Default.BugReport,
                content = { CustomDebugScreen() },
            )
        }

        val plugins = remember(prefsPlugin, networkPlugin, logPlugin, buildInfoPlugin, demoScreen2Plugin) {
            listOf(prefsPlugin, networkPlugin, logPlugin, buildInfoPlugin, demoScreen2Plugin)
        }

        MaterialTheme(colorScheme = colorScheme, typography = AppTypography) {
            SidekickShell(plugins = plugins) {
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
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { DetailPlaceholder() },
                ),
            ) {
                PokemonListScreen(
                    columns = gridColumns,
                    showNumbers = showNumbers,
                    onSelect = { entry ->
                        backStack.removeAll { it is PokemonDetailDestination }
                        backStack.add(PokemonDetailDestination(entry.id, entry.name))
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

// ── Detail pane placeholder (shown when no Pokémon is selected in split view) ─

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

// ── Example CustomScreenPlugin content composables ────────────────────────────

@Composable
private fun BuildInfoScreen() {
    val rows = listOf(
        "Module" to ":demo-app",
        "Kotlin" to "2.3.20",
        "Compose" to "1.10.3",
        "Min SDK" to "24",
    )
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            rows.forEachIndexed { index, (label, value) ->
                ListItem(
                    headlineContent = { Text(label) },
                    trailingContent = {
                        Text(value, style = MaterialTheme.typography.bodyMedium)
                    },
                )
                if (index < rows.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun CustomDebugScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Your custom debug screen",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Replace this composable with your own UI.\nDI (Koin, Hilt, …) works here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

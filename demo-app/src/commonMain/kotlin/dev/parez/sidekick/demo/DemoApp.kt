package dev.parez.sidekick.demo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.parez.sidekick.Sidekick
import dev.parez.sidekick.demo.di.LibraryKoinContext
import dev.parez.sidekick.demo.navigation.PokemonDetailDestination
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
import org.koin.compose.KoinIsolatedContext

// ── Root composable ───────────────────────────────────────────────────────────

@Composable
fun DemoApp() {
    KoinIsolatedContext(context = LibraryKoinContext.koinApp) {
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
            var sidekickVisible by remember { mutableStateOf(false) }

            Box(Modifier.fillMaxSize()) {
                PokemonCatalog(
                    showNumbers = showNumbers,
                    gridColumns = gridColumns,
                )

                SmallFloatingActionButton(
                    onClick = { sidekickVisible = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                ) {
                    Icon(Icons.Filled.BugReport, contentDescription = "Open Sidekick")
                }

                AnimatedVisibility(
                    visible = sidekickVisible,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                ) {
                    Sidekick(
                        plugins = plugins,
                        onClose = { sidekickVisible = false },
                    )
                }
            }
        }
    }
}

// ── Catalog (adaptive list-detail with ListDetailPaneScaffold) ───────────────

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun PokemonCatalog(
    showNumbers: Boolean,
    gridColumns: Int,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<PokemonDetailDestination>()
    val scope = rememberCoroutineScope()

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                PokemonListScreen(
                    columns = gridColumns,
                    showNumbers = showNumbers,
                    onSelect = { entry ->
                        scope.launch {
                            navigator.navigateTo(
                                pane = ListDetailPaneScaffoldRole.Detail,
                                contentKey = PokemonDetailDestination(entry.id, entry.name),
                            )
                        }
                    },
                )
            }
        },
        detailPane = {
            AnimatedPane {
                val destination = navigator.currentDestination?.contentKey
                if (destination != null) {
                    PokemonDetailScreen(
                        id = destination.id,
                        name = destination.name,
                        onBack = { scope.launch { navigator.navigateBack() } },
                    )
                } else {
                    DetailPlaceholder()
                }
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

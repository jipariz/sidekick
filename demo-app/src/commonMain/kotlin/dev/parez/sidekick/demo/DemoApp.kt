package dev.parez.sidekick.demo

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.parez.sidekick.SidekickShell
import dev.parez.sidekick.demo.di.appModule
import dev.parez.sidekick.demo.theme.colorSchemeFor
import dev.parez.sidekick.demo.ui.PokemonDetailScreen
import dev.parez.sidekick.demo.ui.PokemonListScreen
import dev.parez.sidekick.network.NetworkMonitorPlugin
import dev.parez.sidekick.network.RetentionPeriod
import org.koin.compose.KoinApplication

// ── Navigation ────────────────────────────────────────────────────────────────

private sealed class Screen {
    object List : Screen()
    data class Detail(val id: Int, val name: String) : Screen()
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

        MaterialTheme(colorScheme = colorScheme) {
            SidekickShell(plugins = listOf(prefsPlugin, networkPlugin)) {
                PokemonCatalog(
                    showNumbers = showNumbers,
                    gridColumns = gridColumns,
                )
            }
        }
    }
}

// ── Catalog (navigation host) ─────────────────────────────────────────────────

@Composable
private fun PokemonCatalog(
    showNumbers: Boolean,
    gridColumns: Int,
) {
    var screen by remember { mutableStateOf<Screen>(Screen.List) }

    when (val s = screen) {
        is Screen.List -> PokemonListScreen(
            columns = gridColumns,
            showNumbers = showNumbers,
            onSelect = { entry -> screen = Screen.Detail(entry.id, entry.name) },
        )
        is Screen.Detail -> PokemonDetailScreen(
            id = s.id,
            name = s.name,
            onBack = { screen = Screen.List },
        )
    }
}

package dev.parez.sidekick.demo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter
import com.svenjacobs.reveal.OnClick
import com.svenjacobs.reveal.Reveal
import com.svenjacobs.reveal.RevealCanvas
import com.svenjacobs.reveal.RevealShape
import com.svenjacobs.reveal.rememberRevealCanvasState
import com.svenjacobs.reveal.rememberRevealState
import dev.parez.sidekick.Sidekick
import dev.parez.sidekick.demo.di.LibraryKoinContext
import dev.parez.sidekick.demo.theme.AppTypography
import dev.parez.sidekick.demo.theme.colorSchemeFor
import dev.parez.sidekick.logs.LogMonitorPlugin
import dev.parez.sidekick.logs.kermit.LogMonitorLogWriter
import dev.parez.sidekick.network.NetworkMonitorPlugin
import dev.parez.sidekick.screens.CustomScreenPlugin
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.KoinIsolatedContext

internal enum class RevealKey { SidekickFab }

@Composable
fun DemoApp() {
    KoinIsolatedContext(context = LibraryKoinContext.koinApp) {
        val prefsPlugin = remember { AppPreferencesPlugin() }
        val networkPlugin = remember { NetworkMonitorPlugin(retentionPeriod = 1.hours) }
        val logPlugin = remember {
            LogMonitorPlugin(retentionPeriod = 1.hours).also { plugin ->
                Logger.setLogWriters(platformLogWriter(), LogMonitorLogWriter(plugin.store))
            }
        }

        val darkMode by prefsPlugin.accessor.darkMode.collectAsState()
        val colorTheme by prefsPlugin.accessor.colorTheme.collectAsState()
        val showNumbers by prefsPlugin.accessor.showNumbers.collectAsState()
        val shinySprites by prefsPlugin.accessor.shinySprites.collectAsState()

        val colorScheme = colorSchemeFor(theme = colorTheme, dark = darkMode)

        val buildInfoPlugin = remember {
            CustomScreenPlugin(
                id = "build-info",
                title = "Build Info",
                icon = Icons.Default.Info,
                content = { BuildInfoScreen() },
            )
        }
        val customDebugPlugin = remember {
            CustomScreenPlugin(
                id = "custom-debug",
                title = "Custom Debug",
                icon = Icons.Default.BugReport,
                content = { CustomDebugScreen() },
            )
        }

        val plugins = remember(prefsPlugin, networkPlugin, logPlugin, buildInfoPlugin, customDebugPlugin) {
            listOf(prefsPlugin, networkPlugin, logPlugin, buildInfoPlugin, customDebugPlugin)
        }

        MaterialTheme(colorScheme = colorScheme, typography = AppTypography) {
            var sidekickVisible by remember { mutableStateOf(false) }
            val revealCanvasState = rememberRevealCanvasState()
            val revealState = rememberRevealState()
            val revealScope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                if (revealState.isVisible) return@LaunchedEffect
                delay(1.seconds)
                revealState.reveal(RevealKey.SidekickFab)
            }

            RevealCanvas(
                modifier = Modifier.fillMaxSize(),
                revealCanvasState = revealCanvasState,
            ) {
                Reveal(
                    revealCanvasState = revealCanvasState,
                    revealState = revealState,
                    onRevealableClick = { revealScope.launch { revealState.hide() } },
                    onOverlayClick = { revealScope.launch { revealState.hide() } },
                    overlayContent = { key -> SidekickRevealOverlay(key as? RevealKey) },
                ) {
                    Scaffold(
                        floatingActionButton = {
                            if (!sidekickVisible) {
                                SmallFloatingActionButton(
                                    onClick = { sidekickVisible = true },
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .revealable(
                                            key = RevealKey.SidekickFab,
                                            shape = RevealShape.RoundRect(16.dp),
                                            borderStroke = BorderStroke(2.dp, Color.DarkGray),
                                            onClick = OnClick.Listener {
                                                revealScope.launch { revealState.hide() }
                                                sidekickVisible = true
                                            },
                                        ),
                                ) {
                                    Icon(Icons.Filled.BugReport, contentDescription = "Open Sidekick")
                                }
                            }
                        },
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            PokemonCatalog(
                                showNumbers = showNumbers,
                                shinySprites = shinySprites,
                            )

                            AnimatedVisibility(
                                visible = sidekickVisible,
                                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                            ) {
                                Sidekick(
                                    useSidekickTheme = false,
                                    plugins = plugins,
                                    actions = {
                                        IconButton(
                                            onClick = { sidekickVisible = false },
                                            modifier = Modifier.padding(16.dp),
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = "Close Sidekick")
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

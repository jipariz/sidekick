package dev.parez.sidekick.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import dev.parez.sidekick.plugin.LocalSidekickColors
import dev.parez.sidekick.plugin.SidekickColors
import dev.parez.sidekick.plugin.sidekickColors

/**
 * Default dark Material 3 color scheme for the Sidekick overlay.
 * Used as a fallback when the host app has not applied a custom MaterialTheme.
 */
val SidekickDefaultColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFBAC3FF),
    onPrimary = Color(0xFF15267B),
    primaryContainer = Color(0xFF5C6BC0),
    onPrimaryContainer = Color(0xFFF8F6FF),
    secondary = Color(0xFF66D9CC),
    onSecondary = Color(0xFF003732),
    secondaryContainer = Color(0xFF1EA296),
    onSecondaryContainer = Color(0xFF00302B),
    tertiary = Color(0xFFFFBA38),
    onTertiary = Color(0xFF432C00),
    tertiaryContainer = Color(0xFF976900),
    onTertiaryContainer = Color(0xFFFFF6EF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF131313),
    onBackground = Color(0xFFE5E2E1),
    surface = Color(0xFF131313),
    onSurface = Color(0xFFE5E2E1),
    surfaceVariant = Color(0xFF353534),
    onSurfaceVariant = Color(0xFFC6C5D3),
    outline = Color(0xFF8F909D),
    outlineVariant = Color(0xFF454651),
    surfaceContainer = Color(0xFF201F1F),
    surfaceContainerHigh = Color(0xFF2A2A2A),
    surfaceContainerHighest = Color(0xFF353534),
    surfaceContainerLow = Color(0xFF1C1B1B),
    surfaceContainerLowest = Color(0xFF0E0E0E),
    inverseSurface = Color(0xFFE5E2E1),
    inverseOnSurface = Color(0xFF313030),
    inversePrimary = Color(0xFF4858AB),
)

/**
 * Light variant of the Sidekick library color scheme.
 */
val SidekickLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF4A5798),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF001258),
    secondary = Color(0xFF006B60),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF9EF2E4),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = Color(0xFF7D5700),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDEA0),
    onTertiaryContainer = Color(0xFF271900),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE2E1EC),
    onSurfaceVariant = Color(0xFF45464F),
    outline = Color(0xFF767680),
    outlineVariant = Color(0xFFC6C5D0),
    surfaceContainer = Color(0xFFF0EDF5),
    surfaceContainerHigh = Color(0xFFEAE7EF),
    surfaceContainerHighest = Color(0xFFE4E1EA),
    surfaceContainerLow = Color(0xFFF6F2FA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    inverseSurface = Color(0xFF303036),
    inverseOnSurface = Color(0xFFF3EFF7),
    inversePrimary = Color(0xFFBAC3FF),
)

/**
 * Signals that a [SidekickTheme] has been explicitly applied in this subtree.
 * Prevents [SidekickShell] from overriding an intentional theme with the fallback.
 */
internal val LocalSidekickThemeActive = compositionLocalOf { false }

/**
 * Flexible theme wrapper for the Sidekick library.
 *
 * When [useLibraryTheme] is `true` (default), Sidekick applies its own
 * [lightColorScheme][SidekickLightColorScheme] / [darkColorScheme][SidekickDefaultColorScheme]
 * based on the system dark-mode setting. When `false`, the host application's ambient
 * [MaterialTheme] is inherited as-is — only the [SidekickColors] semantic tokens are provided.
 *
 * Use this at the entry point of Sidekick's UI to give host apps control over theming:
 * ```kotlin
 * // Library's own palette
 * LibraryTheme { SidekickShell(plugins) { MyApp() } }
 *
 * // Inherit host's MaterialTheme
 * LibraryTheme(useLibraryTheme = false) { SidekickShell(plugins) { MyApp() } }
 * ```
 *
 * @param useLibraryTheme When true, apply the library's color scheme; when false,
 *                        pass through the host's ambient MaterialTheme.
 * @param sidekickColors  Sidekick semantic tokens. When null, auto-derived from the active scheme.
 */
@Composable
fun LibraryTheme(
    useLibraryTheme: Boolean = true,
    sidekickColors: SidekickColors? = null,
    content: @Composable () -> Unit,
) {
    if (useLibraryTheme) {
        val colorScheme = if (isSystemInDarkTheme()) SidekickDefaultColorScheme else SidekickLightColorScheme
        MaterialTheme(colorScheme = colorScheme) {
            val resolvedColors = sidekickColors ?: sidekickColors()
            CompositionLocalProvider(
                LocalSidekickColors provides resolvedColors,
                LocalSidekickThemeActive provides true,
            ) {
                content()
            }
        }
    } else {
        // Pass through host's MaterialTheme, only provide Sidekick semantic tokens
        val resolvedColors = sidekickColors ?: sidekickColors()
        CompositionLocalProvider(
            LocalSidekickColors provides resolvedColors,
            LocalSidekickThemeActive provides true,
        ) {
            content()
        }
    }
}

/**
 * Explicit Sidekick theme wrapper with a specific color scheme.
 *
 * @param colorScheme    Explicit Material 3 color scheme. Defaults to [SidekickDefaultColorScheme].
 * @param sidekickColors Sidekick semantic tokens. When null, auto-derived from [colorScheme].
 */
@Composable
fun SidekickTheme(
    colorScheme: ColorScheme = SidekickDefaultColorScheme,
    sidekickColors: SidekickColors? = null,
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = colorScheme) {
        val resolvedColors = sidekickColors ?: sidekickColors()
        CompositionLocalProvider(
            LocalSidekickColors provides resolvedColors,
            LocalSidekickThemeActive provides true,
        ) {
            content()
        }
    }
}

// ─── Internal helpers ────────────────────────────────────────────────────────

private val m3DefaultLight = lightColorScheme()
private val m3DefaultDark = darkColorScheme()

internal fun ColorScheme.isM3Default(): Boolean {
    return (primary == m3DefaultLight.primary
            && secondary == m3DefaultLight.secondary
            && tertiary == m3DefaultLight.tertiary)
        || (primary == m3DefaultDark.primary
            && secondary == m3DefaultDark.secondary
            && tertiary == m3DefaultDark.tertiary)
}

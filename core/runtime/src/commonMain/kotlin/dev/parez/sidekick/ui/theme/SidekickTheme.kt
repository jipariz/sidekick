package dev.parez.sidekick.ui.theme

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
 * Default Material 3 color scheme for the Sidekick overlay.
 * Mirrors the "Sidekick Material Adaptive" Stitch design system (indigo / teal / amber, dark).
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
 * Signals that a [SidekickTheme] has been explicitly applied in this subtree.
 * Prevents [SidekickShell] from overriding an intentional theme with the fallback.
 */
internal val LocalSidekickThemeActive = compositionLocalOf { false }

/**
 * Explicit Sidekick theme wrapper. Use this when you want Sidekick's own design applied
 * to a scope, regardless of the host app's MaterialTheme:
 *
 * ```kotlin
 * // Force Sidekick's dark design for the whole shell
 * SidekickTheme {
 *     SidekickShell(plugins) { MyApp() }
 * }
 *
 * // Or use the host's scheme but override only HTTP badge colors
 * SidekickTheme(
 *     colorScheme = MaterialTheme.colorScheme,
 *     sidekickColors = sidekickColors(httpDelete = Color.Red),
 * ) { ... }
 * ```
 *
 * When used inside [SidekickShell], the [SidekickShell]'s auto-detection is bypassed and
 * this theme wins.
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

/**
 * The primary/secondary/tertiary triple from Material 3's unmodified [lightColorScheme].
 * Comparing against these three values is sufficient to detect an uncustomised default.
 */
private val m3DefaultLight = lightColorScheme()

/**
 * The primary/secondary/tertiary triple from Material 3's unmodified [darkColorScheme].
 */
private val m3DefaultDark = darkColorScheme()

/**
 * Returns true if this [ColorScheme] looks like one of the unmodified Material 3 defaults
 * (light or dark). Checking primary + secondary + tertiary together makes false positives
 * practically impossible in real apps.
 */
internal fun ColorScheme.isM3Default(): Boolean {
    return (primary == m3DefaultLight.primary
            && secondary == m3DefaultLight.secondary
            && tertiary == m3DefaultLight.tertiary)
        || (primary == m3DefaultDark.primary
            && secondary == m3DefaultDark.secondary
            && tertiary == m3DefaultDark.tertiary)
}

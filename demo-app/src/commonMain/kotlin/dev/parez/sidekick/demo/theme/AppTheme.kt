package dev.parez.sidekick.demo.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import dev.parez.sidekick.demo.ColorTheme

@Composable
fun colorSchemeFor(theme: ColorTheme, dark: Boolean): ColorScheme = when (theme) {
    ColorTheme.DYNAMIC   -> dynamicColorScheme(dark) ?: if (dark) defaultDark() else defaultLight()
    ColorTheme.FIRE      -> if (dark) fireDark()      else fireLight()
    ColorTheme.WATER     -> if (dark) waterDark()     else waterLight()
    ColorTheme.GRASS     -> if (dark) grassDark()     else grassLight()
    ColorTheme.ELECTRIC  -> if (dark) electricDark()  else electricLight()
    ColorTheme.PSYCHIC   -> if (dark) psychicDark()   else psychicLight()
    ColorTheme.DEFAULT   -> if (dark) defaultDark()   else defaultLight()
}

/**
 * Returns the platform's dynamic color scheme, or null if unavailable.
 * On Android 12+ this uses the user's wallpaper-derived colors.
 */
@Composable
expect fun dynamicColorScheme(dark: Boolean): ColorScheme?

// ── Typography ───────────────────────────────────────────────────────────────

private val defaultTypography = Typography()

/**
 * App typography with emphasized display/headline weights for brand expression.
 */
val AppTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontWeight = FontWeight.Bold),
    displayMedium = defaultTypography.displayMedium.copy(fontWeight = FontWeight.Bold),
    displaySmall = defaultTypography.displaySmall.copy(fontWeight = FontWeight.SemiBold),
    headlineLarge = defaultTypography.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = defaultTypography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = defaultTypography.headlineSmall.copy(fontWeight = FontWeight.Medium),
    titleLarge = defaultTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
)

// ── Default (Indigo / Teal) ───────────────────────────────────────────────────

private fun defaultLight() = lightColorScheme(
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
)

private fun defaultDark() = darkColorScheme(
    primary = Color(0xFFBAC3FF),
    onPrimary = Color(0xFF15267B),
    primaryContainer = Color(0xFF323F8F),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFF82D5C8),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF005048),
    onSecondaryContainer = Color(0xFF9EF2E4),
    tertiary = Color(0xFFEDBC55),
    onTertiary = Color(0xFF412D00),
    tertiaryContainer = Color(0xFF5D4100),
    onTertiaryContainer = Color(0xFFFFDEA0),
)

// ── Fire (Red / Orange) ───────────────────────────────────────────────────────

private fun fireLight() = lightColorScheme(
    primary = Color(0xFFB71C1C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFFBF360C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBCC),
    onSecondaryContainer = Color(0xFF3B0900),
    tertiary = Color(0xFF7D5700),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDEA0),
    onTertiaryContainer = Color(0xFF271900),
)

private fun fireDark() = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF93000A),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFFFB59A),
    onSecondary = Color(0xFF5B1A00),
    secondaryContainer = Color(0xFF7D2900),
    onSecondaryContainer = Color(0xFFFFDBCC),
    tertiary = Color(0xFFEDBC55),
    onTertiary = Color(0xFF412D00),
    tertiaryContainer = Color(0xFF5D4100),
    onTertiaryContainer = Color(0xFFFFDEA0),
)

// ── Water (Blue / Cyan) ───────────────────────────────────────────────────────

private fun waterLight() = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E4FF),
    onPrimaryContainer = Color(0xFF001C3B),
    secondary = Color(0xFF006064),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF9CF0F2),
    onSecondaryContainer = Color(0xFF001F20),
    tertiary = Color(0xFF4E4799),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE5DEFF),
    onTertiaryContainer = Color(0xFF100451),
)

private fun waterDark() = darkColorScheme(
    primary = Color(0xFFA4C8FF),
    onPrimary = Color(0xFF003060),
    primaryContainer = Color(0xFF004A93),
    onPrimaryContainer = Color(0xFFD3E4FF),
    secondary = Color(0xFF80D4D8),
    onSecondary = Color(0xFF003436),
    secondaryContainer = Color(0xFF00494C),
    onSecondaryContainer = Color(0xFF9CF0F2),
    tertiary = Color(0xFFC8BFFF),
    onTertiary = Color(0xFF1F1667),
    tertiaryContainer = Color(0xFF362F80),
    onTertiaryContainer = Color(0xFFE5DEFF),
)

// ── Grass (Green / Lime) ──────────────────────────────────────────────────────

private fun grassLight() = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB6F0B1),
    onPrimaryContainer = Color(0xFF002204),
    secondary = Color(0xFF558B2F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCEDA5),
    onSecondaryContainer = Color(0xFF102000),
    tertiary = Color(0xFF006A60),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF9EF2E4),
    onTertiaryContainer = Color(0xFF00201C),
)

private fun grassDark() = darkColorScheme(
    primary = Color(0xFF9BD497),
    onPrimary = Color(0xFF00390A),
    primaryContainer = Color(0xFF165220),
    onPrimaryContainer = Color(0xFFB6F0B1),
    secondary = Color(0xFFB0D18C),
    onSecondary = Color(0xFF1F3701),
    secondaryContainer = Color(0xFF354E17),
    onSecondaryContainer = Color(0xFFCCEDA5),
    tertiary = Color(0xFF82D5C8),
    onTertiary = Color(0xFF003731),
    tertiaryContainer = Color(0xFF005048),
    onTertiaryContainer = Color(0xFF9EF2E4),
)

// ── Electric (Amber / Yellow) ─────────────────────────────────────────────────

private fun electricLight() = lightColorScheme(
    primary = Color(0xFFF57F17),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDEA0),
    onPrimaryContainer = Color(0xFF271900),
    secondary = Color(0xFF1565C0),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD3E4FF),
    onSecondaryContainer = Color(0xFF001C3B),
    tertiary = Color(0xFF6D1699),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF2DAFF),
    onTertiaryContainer = Color(0xFF280050),
)

private fun electricDark() = darkColorScheme(
    primary = Color(0xFFFFBA4D),
    onPrimary = Color(0xFF422C00),
    primaryContainer = Color(0xFF5F4100),
    onPrimaryContainer = Color(0xFFFFDEA0),
    secondary = Color(0xFFA4C8FF),
    onSecondary = Color(0xFF003060),
    secondaryContainer = Color(0xFF004A93),
    onSecondaryContainer = Color(0xFFD3E4FF),
    tertiary = Color(0xFFDFB4FF),
    onTertiary = Color(0xFF450069),
    tertiaryContainer = Color(0xFF5B0081),
    onTertiaryContainer = Color(0xFFF2DAFF),
)

// ── Psychic (Pink / Purple) ───────────────────────────────────────────────────

private fun psychicLight() = lightColorScheme(
    primary = Color(0xFF880E4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD8E7),
    onPrimaryContainer = Color(0xFF3B0018),
    secondary = Color(0xFF4A148C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFECDCFF),
    onSecondaryContainer = Color(0xFF22005D),
    tertiary = Color(0xFF006A60),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF9EF2E4),
    onTertiaryContainer = Color(0xFF00201C),
)

private fun psychicDark() = darkColorScheme(
    primary = Color(0xFFFFB0CB),
    onPrimary = Color(0xFF64002F),
    primaryContainer = Color(0xFF8C0043),
    onPrimaryContainer = Color(0xFFFFD8E7),
    secondary = Color(0xFFD3BAFF),
    onSecondary = Color(0xFF350072),
    secondaryContainer = Color(0xFF4D009E),
    onSecondaryContainer = Color(0xFFECDCFF),
    tertiary = Color(0xFF82D5C8),
    onTertiary = Color(0xFF003731),
    tertiaryContainer = Color(0xFF005048),
    onTertiaryContainer = Color(0xFF9EF2E4),
)

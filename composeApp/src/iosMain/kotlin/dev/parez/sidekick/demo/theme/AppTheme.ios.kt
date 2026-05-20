package dev.parez.sidekick.demo.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * iOS does not expose a system-derived "dynamic" Material You scheme
 * (that's an Android 12+ wallpaper-extraction API). Returning null falls
 * back to one of the curated themes in [colorSchemeFor].
 */
@Composable
actual fun dynamicColorScheme(dark: Boolean): ColorScheme? = null

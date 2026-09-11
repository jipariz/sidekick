package dev.parez.sidekick.plugin

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

/**
 * Provides a callback that navigates back to the Sidekick plugin list.
 *
 * Populated by the Sidekick runtime inside [dev.parez.sidekick.ui.SidekickPluginScreen]. Plugin
 * implementations call [current][androidx.compose.runtime.ProvidableCompositionLocal.current] to
 * obtain the lambda and invoke it from a back button or similar affordance.
 *
 * ```kotlin
 * @Composable
 * override fun Content() {
 *     val navigateBack = LocalSidekickBackNavigator.current
 *     IconButton(onClick = navigateBack) { ... }
 * }
 * ```
 */
public val LocalSidekickBackNavigator: ProvidableCompositionLocal<() -> Unit> =
    compositionLocalOf<() -> Unit> {
        error(
            "LocalSidekickBackNavigator not provided — is this composable running inside Sidekick?"
        )
    }

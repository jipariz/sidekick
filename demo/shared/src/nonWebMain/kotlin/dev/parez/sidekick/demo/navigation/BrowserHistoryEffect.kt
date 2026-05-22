package dev.parez.sidekick.demo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

@Composable
actual fun BrowserHistoryEffect(backStack: NavBackStack<NavKey>) {
    // Non-web targets have no browser History to bind against.
}

package dev.parez.sidekick.demo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * On web targets, binds the nav3 [backStack] to the browser History API so URL hash, back/forward,
 * and deep links stay in sync with the in-app navigation.
 *
 * Non-web targets get a no-op actual — there's no browser to talk to.
 */
@Composable expect fun BrowserHistoryEffect(backStack: NavBackStack<NavKey>)

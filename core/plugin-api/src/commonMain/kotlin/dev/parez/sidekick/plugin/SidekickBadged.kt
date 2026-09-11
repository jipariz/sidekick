package dev.parez.sidekick.plugin

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State

/**
 * Optional extension for [SidekickPlugin] implementations that can report a count of items the user
 * has not looked at yet — new requests, new log entries, a crash that just landed.
 *
 * Sidekick draws this as a badge on the plugin's card in the grid. The host app can read the same
 * value to badge its own entry point, which is the more useful half: the host owns the FAB, so
 * without this there is no way for an overlay to signal that something happened while it was
 * closed.
 *
 * ```kotlin
 * class MyPlugin : SidekickPlugin, SidekickBadged {
 *     private val _badge = mutableStateOf<Int?>(null)
 *     override val badge: State<Int?> = _badge
 *     // …
 * }
 *
 * // In the host app:
 * val count by myPlugin.badge
 * BadgedBox(badge = { if (count != null) Badge { Text("$count") } }) { Fab() }
 * ```
 *
 * Declared as a separate interface rather than a member of [SidekickPlugin] so adding it does not
 * break existing implementations — the same reason [SidekickLifecycleAware] is separate.
 */
@Stable
public interface SidekickBadged {
    /**
     * Unread count, or `null` when there is nothing to show. Read from composition, so implementers
     * should back this with snapshot state (`mutableStateOf`) rather than a plain field.
     */
    public val badge: State<Int?>
}

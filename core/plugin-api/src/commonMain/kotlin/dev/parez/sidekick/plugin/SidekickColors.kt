package dev.parez.sidekick.plugin

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic color tokens specific to Sidekick's UI (HTTP method badges, status chips).
 *
 * All colors default to [MaterialTheme.colorScheme] equivalents so the overlay
 * automatically adapts to whatever Material theme the host provides via [SidekickTheme].
 * Override individual colors by passing a custom [SidekickColors] to [SidekickTheme].
 */
data class SidekickColors(
    // HTTP method badge backgrounds
    val httpGet: Color,
    val httpPost: Color,
    val httpPut: Color,
    val httpDelete: Color,
    val httpPatch: Color,
    val httpOther: Color,
    /** Text/icon color drawn on top of any HTTP method badge. */
    val onHttpBadge: Color,
    // Response status chip backgrounds
    val statusSuccess: Color,      // 2xx
    val statusRedirect: Color,     // 3xx
    val statusClientError: Color,  // 4xx
    val statusServerError: Color,  // 5xx
    val statusPending: Color,
    val statusNetworkError: Color,
    /** Text/icon color drawn on top of any status chip. */
    val onStatusChip: Color,
)

/**
 * CompositionLocal carrying [SidekickColors]. Populated by [SidekickTheme]; throws if
 * accessed outside a [SidekickTheme] context.
 */
val LocalSidekickColors = staticCompositionLocalOf<SidekickColors> {
    error("No SidekickColors provided. Wrap your Sidekick content in SidekickTheme.")
}

/**
 * Creates a [SidekickColors] with defaults derived from the current [MaterialTheme].
 * Must be called inside a composition with an active [MaterialTheme].
 *
 * @param httpGet         Badge color for GET requests. Defaults to [MaterialTheme.colorScheme.primary].
 * @param httpPost        Badge color for POST requests. Defaults to [MaterialTheme.colorScheme.secondary].
 * @param httpPut         Badge color for PUT requests. Defaults to [MaterialTheme.colorScheme.tertiary].
 * @param httpDelete      Badge color for DELETE requests. Defaults to [MaterialTheme.colorScheme.error].
 * @param httpPatch       Badge color for PATCH requests. Defaults to [MaterialTheme.colorScheme.tertiaryContainer].
 * @param httpOther       Badge color for all other methods. Defaults to [MaterialTheme.colorScheme.outline].
 * @param onHttpBadge     Text color on all method badges. Defaults to [Color.White].
 * @param statusSuccess   Chip color for 2xx responses. Defaults to [MaterialTheme.colorScheme.secondary].
 * @param statusRedirect  Chip color for 3xx responses. Defaults to [MaterialTheme.colorScheme.primary].
 * @param statusClientError Chip color for 4xx responses. Defaults to [MaterialTheme.colorScheme.tertiary].
 * @param statusServerError Chip color for 5xx responses. Defaults to [MaterialTheme.colorScheme.error].
 * @param statusPending   Chip color for in-flight requests. Defaults to [MaterialTheme.colorScheme.outlineVariant].
 * @param statusNetworkError Chip color for network errors. Defaults to [MaterialTheme.colorScheme.error].
 * @param onStatusChip    Text color on all status chips. Defaults to [Color.White].
 */
@Composable
fun sidekickColors(
    httpGet: Color = MaterialTheme.colorScheme.primary,
    httpPost: Color = MaterialTheme.colorScheme.secondary,
    httpPut: Color = MaterialTheme.colorScheme.tertiary,
    httpDelete: Color = MaterialTheme.colorScheme.error,
    httpPatch: Color = MaterialTheme.colorScheme.tertiaryContainer,
    httpOther: Color = MaterialTheme.colorScheme.outline,
    onHttpBadge: Color = Color.White,
    statusSuccess: Color = MaterialTheme.colorScheme.secondary,
    statusRedirect: Color = MaterialTheme.colorScheme.primary,
    statusClientError: Color = MaterialTheme.colorScheme.tertiary,
    statusServerError: Color = MaterialTheme.colorScheme.error,
    statusPending: Color = MaterialTheme.colorScheme.outlineVariant,
    statusNetworkError: Color = MaterialTheme.colorScheme.error,
    onStatusChip: Color = Color.White,
): SidekickColors = SidekickColors(
    httpGet = httpGet,
    httpPost = httpPost,
    httpPut = httpPut,
    httpDelete = httpDelete,
    httpPatch = httpPatch,
    httpOther = httpOther,
    onHttpBadge = onHttpBadge,
    statusSuccess = statusSuccess,
    statusRedirect = statusRedirect,
    statusClientError = statusClientError,
    statusServerError = statusServerError,
    statusPending = statusPending,
    statusNetworkError = statusNetworkError,
    onStatusChip = onStatusChip,
)

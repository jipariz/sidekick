package dev.parez.sidekick.plugin

import kotlinx.browser.window

/**
 * Auto-detects [SidekickAppInfo] from the browser environment.
 *
 * Captures the user-agent string and parses the browser name in [PlatformInfo.Web].
 * App-level info (version, build type) is not available at runtime in the browser; use
 * [withExtras] to surface those values:
 * ```kotlin
 * SidekickAppInfo.detect().withExtras("Version" to "1.0.0", "Build" to "release")
 * ```
 */
actual fun SidekickAppInfo.Companion.detect(): SidekickAppInfo {
    val ua = window.navigator.userAgent
    return SidekickAppInfo(
        platform = PlatformInfo.Web(
            userAgent = ua,
            browserName = parseBrowserName(ua),
        ),
    )
}

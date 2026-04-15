package dev.parez.sidekick.plugin

/**
 * Parses a human-readable browser name from a `navigator.userAgent` string.
 * Order matters: Edge and Opera both contain "Chrome", so they must be checked first.
 */
internal fun parseBrowserName(ua: String): String? = when {
    ua.contains("Edg/") || ua.contains("EdgA/") -> "Edge"
    ua.contains("OPR/") || ua.contains("Opera/") -> "Opera"
    ua.contains("SamsungBrowser/") -> "Samsung Internet"
    ua.contains("Chrome/") -> "Chrome"
    ua.contains("Firefox/") -> "Firefox"
    ua.contains("Safari/") && ua.contains("Version/") -> "Safari"
    else -> null
}

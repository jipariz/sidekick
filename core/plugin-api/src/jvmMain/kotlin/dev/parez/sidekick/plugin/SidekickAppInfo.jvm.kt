package dev.parez.sidekick.plugin

/**
 * Auto-detects [SidekickAppInfo] from the JVM runtime.
 *
 * Captures OS name/version, JVM version, and processor count in [PlatformInfo.Desktop].
 * App-level info (version, build type) is not available at runtime on Desktop; use
 * [withExtras] to surface those values:
 * ```kotlin
 * SidekickAppInfo.detect().withExtras("Version" to "1.0.0", "Build" to "debug")
 * ```
 */
actual fun SidekickAppInfo.Companion.detect(): SidekickAppInfo = SidekickAppInfo(
    platform = PlatformInfo.Desktop(
        osName = System.getProperty("os.name") ?: "Unknown",
        osVersion = System.getProperty("os.version") ?: "Unknown",
        jvmVersion = System.getProperty("java.version") ?: "Unknown",
        availableProcessors = Runtime.getRuntime().availableProcessors(),
    ),
)

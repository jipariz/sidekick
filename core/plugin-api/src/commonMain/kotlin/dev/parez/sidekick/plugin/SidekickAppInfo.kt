package dev.parez.sidekick.plugin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Host-application metadata shown in the Sidekick overlay header.
 *
 * App-level info (name, version, build type) lives inside [PlatformInfo] subtypes rather than
 * here, because only Android and iOS can auto-detect those values. Desktop and Web expose
 * only OS/runtime info; use [withExtras] to surface any additional key-value pairs.
 *
 * Obtain via [SidekickAppInfo.detect] (auto-detects the current platform) or construct manually.
 */
data class SidekickAppInfo(
    /** Platform-specific metadata. Defaults to [PlatformInfo.Unknown]. */
    val platform: PlatformInfo = PlatformInfo.Unknown,
    /**
     * Optional custom key-value entries shown as additional badges in the Sidekick header.
     * Prefer [withExtras] over `.copy(extras = …)` to ensure auto-detected fields are preserved.
     */
    val extras: Map<String, String> = emptyMap(),
) {
    companion object
}

/**
 * Returns a copy of this [SidekickAppInfo] with the given key-value pairs appended to [extras].
 * All auto-detected platform fields are preserved unchanged.
 *
 * ```kotlin
 * SidekickShell(
 *     plugins = plugins,
 *     appInfo = SidekickAppInfo.detect().withExtras(
 *         "Environment" to "staging",
 *         "Region" to "EU-West",
 *     ),
 * ) { ... }
 * ```
 */
fun SidekickAppInfo.withExtras(vararg pairs: Pair<String, String>): SidekickAppInfo =
    copy(extras = extras + mapOf(*pairs))

/**
 * Auto-detects [SidekickAppInfo] for the current platform.
 *
 * - **Android** — app name, version, build code, build type (debuggable flag), API level, device
 * - **iOS** — app name, version, build code from main bundle; iOS version and device model
 * - **Desktop (JVM)** — OS name/version, JVM version, processor count
 * - **Web** — browser user-agent + parsed browser name
 *
 * Build type cannot be determined at runtime on iOS, Desktop, or Web. Supply it via the
 * platform-specific overload or [withExtras] where needed.
 *
 * To customise without losing auto-detected fields:
 * ```kotlin
 * SidekickAppInfo.detect().withExtras("Environment" to "staging")
 * // or, to override a detected field explicitly:
 * (SidekickAppInfo.detect().platform as? PlatformInfo.Android)
 *     ?.copy(buildType = BuildConfig.BUILD_TYPE)
 *     ?.let { SidekickAppInfo(platform = it) }
 * ```
 * To disable the header entirely, pass `appInfo = null` to [dev.parez.sidekick.SidekickShell].
 */
expect fun SidekickAppInfo.Companion.detect(): SidekickAppInfo

/**
 * Returns a [SidekickAppInfo] that is auto-detected once and remembered across recompositions.
 * Used as the default value for [dev.parez.sidekick.SidekickShell].
 */
@Composable
fun rememberSidekickAppInfo(): SidekickAppInfo = remember { SidekickAppInfo.detect() }

// ── Platform info ─────────────────────────────────────────────────────────────

/**
 * Platform-specific debug metadata surfaced in the Sidekick header.
 *
 * App-level fields (name, version, build type) are included in [Android] and [Ios] because
 * those platforms can auto-detect them. [Desktop] and [Web] expose only runtime/OS info.
 */
sealed interface PlatformInfo {

    data class Android(
        /** Application label from the package manager. */
        val appName: String,
        /** Version string from the package manifest (e.g. "1.2.3"). */
        val appVersion: String?,
        /** Numeric version code from the package manifest. */
        val buildCode: Long?,
        /** Build type, inferred from the debuggable flag or passed explicitly (e.g. "debug"). */
        val buildType: String,
        /** Android API level (e.g. 34). */
        val apiLevel: Int,
        /** Device model from [android.os.Build.MODEL] (e.g. "Pixel 7"). */
        val deviceModel: String,
        /** Manufacturer from [android.os.Build.MANUFACTURER] (e.g. "Google"). */
        val manufacturer: String,
        /** Optional product flavour name (e.g. "dev", "staging"). */
        val buildFlavor: String? = null,
    ) : PlatformInfo

    data class Ios(
        /** App display name from `CFBundleDisplayName` / `CFBundleName`. */
        val appName: String?,
        /** Version from `CFBundleShortVersionString`. */
        val appVersion: String?,
        /** Build number from `CFBundleVersion`. */
        val buildCode: Long?,
        /**
         * Build configuration (e.g. "Debug", "Release"). Cannot be read at runtime;
         * supply via [SidekickAppInfo.Companion.detect].
         */
        val buildType: String? = null,
        /** iOS version from UIDevice (e.g. "17.2"). */
        val systemVersion: String,
        /** Device model from UIDevice (e.g. "iPhone", "iPad"). */
        val deviceModel: String,
    ) : PlatformInfo

    data class Desktop(
        /** OS name from `System.getProperty("os.name")` (e.g. "Mac OS X"). */
        val osName: String,
        /** OS version from `System.getProperty("os.version")` (e.g. "14.5"). */
        val osVersion: String,
        /** JVM version from `System.getProperty("java.version")` (e.g. "21.0.2"). */
        val jvmVersion: String,
        /** Available processor count from [Runtime.availableProcessors]. */
        val availableProcessors: Int,
    ) : PlatformInfo

    data class Web(
        /** Full browser user-agent string from `navigator.userAgent`. */
        val userAgent: String,
        /** Human-readable browser name parsed from [userAgent] (e.g. "Chrome", "Firefox"). */
        val browserName: String? = null,
    ) : PlatformInfo

    /** Used when platform info is unavailable or not provided. Nothing extra is rendered. */
    data object Unknown : PlatformInfo
}

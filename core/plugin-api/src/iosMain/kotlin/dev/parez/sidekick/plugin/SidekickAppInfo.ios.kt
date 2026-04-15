package dev.parez.sidekick.plugin

import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

/**
 * Auto-detects [SidekickAppInfo] from the iOS runtime.
 *
 * - App name, version (`CFBundleShortVersionString`), and build code (`CFBundleVersion`)
 *   are read from the main bundle automatically.
 * - iOS version and device model are read from [UIDevice].
 * - Build type (Debug / Release / …) cannot be determined at runtime; use the [detect]
 *   overload to supply it from your build configuration.
 */
actual fun SidekickAppInfo.Companion.detect(): SidekickAppInfo {
    val device = UIDevice.currentDevice
    val info = NSBundle.mainBundle().infoDictionary

    return SidekickAppInfo(
        platform = PlatformInfo.Ios(
            appName = info?.get("CFBundleDisplayName") as? String
                ?: info?.get("CFBundleName") as? String,
            appVersion = info?.get("CFBundleShortVersionString") as? String,
            buildCode = (info?.get("CFBundleVersion") as? String)?.toLongOrNull(),
            systemVersion = device.systemVersion,
            deviceModel = device.model,
        ),
    )
}

/**
 * Detects [SidekickAppInfo] and sets the build type, which cannot be read at runtime on iOS.
 *
 * ```kotlin
 * SidekickAppInfo.detect(buildType = "Debug")
 * ```
 *
 * @param buildType Build configuration name (e.g. "Debug", "Release", "Staging").
 */
fun SidekickAppInfo.Companion.detect(buildType: String?): SidekickAppInfo {
    val base = detect()
    val p = base.platform as? PlatformInfo.Ios ?: return base
    return base.copy(platform = p.copy(buildType = buildType))
}

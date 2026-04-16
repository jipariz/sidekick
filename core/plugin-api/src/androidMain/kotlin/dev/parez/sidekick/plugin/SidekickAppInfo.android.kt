package dev.parez.sidekick.plugin

import android.content.pm.ApplicationInfo
import android.os.Build

/**
 * Auto-detects [SidekickAppInfo] from the Android runtime.
 *
 * All app-level fields (name, version, build code, build type) are read automatically and
 * stored in [PlatformInfo.Android]. Build type is inferred from the debuggable flag;
 * use the [detect] overload to supply [android.os.Build] config values explicitly.
 *
 * Requires [ApplicationContextHolder] to be initialised in [android.app.Application.onCreate].
 */
actual fun SidekickAppInfo.Companion.detect(): SidekickAppInfo {
    if (!ApplicationContextHolder.isInitialized) return SidekickAppInfo()
    val context = ApplicationContextHolder.context
    val appInfo = context.applicationInfo

    val packageInfo = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }.getOrNull()

    val isDebug = (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    val versionCode: Long? = packageInfo?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            it.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            it.versionCode.toLong()
        }
    }

    return SidekickAppInfo(
        platform = PlatformInfo.Android(
            appName = appInfo.loadLabel(context.packageManager).toString(),
            appVersion = packageInfo?.versionName,
            buildCode = versionCode,
            buildType = if (isDebug) "debug" else "release",
            apiLevel = Build.VERSION.SDK_INT,
            deviceModel = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
        ),
    )
}

/**
 * Detects [SidekickAppInfo] and overrides build metadata with explicit values.
 *
 * Use when `BuildConfig` is available for exact Gradle build-type and flavour names:
 * ```kotlin
 * SidekickAppInfo.detect(
 *     buildType = BuildConfig.BUILD_TYPE,
 *     buildFlavor = BuildConfig.FLAVOR,
 * )
 * ```
 *
 * @param buildType   Overrides the auto-detected build type (e.g. `BuildConfig.BUILD_TYPE`).
 * @param buildFlavor Product flavour name to display (e.g. `BuildConfig.FLAVOR`).
 */
fun SidekickAppInfo.Companion.detect(
    buildType: String? = null,
    buildFlavor: String? = null,
): SidekickAppInfo {
    val base = detect()
    val p = base.platform as? PlatformInfo.Android ?: return base
    return base.copy(
        platform = p.copy(
            buildType = buildType ?: p.buildType,
            buildFlavor = buildFlavor,
        ),
    )
}

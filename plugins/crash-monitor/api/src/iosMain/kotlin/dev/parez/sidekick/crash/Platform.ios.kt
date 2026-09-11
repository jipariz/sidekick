package dev.parez.sidekick.crash

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.setUnhandledExceptionHook
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile
import platform.posix.gettimeofday
import platform.posix.timeval

@OptIn(ExperimentalForeignApi::class)
internal actual object CrashStorage {

    private val path: String by lazy {
        val documents =
            NSSearchPathForDirectoriesInDomains(
                    directory = NSDocumentDirectory,
                    domainMask = NSUserDomainMask,
                    expandTilde = true,
                )
                .firstOrNull() as? String
        documents?.let { "$it/$CRASH_LOG_NAME" } ?: CRASH_LOG_NAME
    }

    actual fun read(): String? =
        NSString.stringWithContentsOfFile(path, encoding = NSUTF8StringEncoding, error = null)

    actual fun append(text: String) {
        // No incremental append API without cinterop file handles; the log is capped
        // at MAX_CRASHES records, so rewriting it whole is cheap and atomic enough.
        val combined = (read() ?: "") + text
        @Suppress("CAST_NEVER_SUCCEEDS")
        (combined as NSString).writeToFile(
            path,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null,
        )
    }

    actual fun clear() {
        NSFileManager.defaultManager.removeItemAtPath(path, null)
    }
}

/**
 * iOS needs *two* hooks, and using only the obvious one is the classic mistake.
 *
 * `NSSetUncaughtExceptionHandler` catches Objective-C exceptions, which a Kotlin/Native app almost
 * never throws. Kotlin failures — the ones an app actually crashes on — go through
 * [setUnhandledExceptionHook] instead. Only the second is wired here because the first cannot
 * observe a Kotlin throwable, which is the only thing this store can record.
 */
@OptIn(ExperimentalNativeApi::class)
internal actual fun installPlatformCrashHandler(onCrash: (Throwable, threadName: String) -> Unit) {
    setUnhandledExceptionHook { throwable -> runCatching { onCrash(throwable, "main") } }
}

internal actual fun currentThreadName(): String = "main"

// Matches the monitors' iOS clock (see network-monitor's TimeMillis.ios.kt) rather
// than NSDate, so all of Sidekick's timestamps come from one source.
@OptIn(ExperimentalForeignApi::class)
internal actual fun currentTimeMillis(): Long = memScoped {
    val tv = alloc<timeval>()
    gettimeofday(tv.ptr, null)
    tv.tv_sec * 1000L + tv.tv_usec / 1000L
}

internal actual fun randomId(): String = NSUUID().UUIDString()

package dev.parez.sidekick.crash

import dev.parez.sidekick.plugin.ApplicationContextHolder
import java.io.File
import java.util.UUID

internal actual object CrashStorage {
    private val file: File?
        get() =
            if (ApplicationContextHolder.isInitialized) {
                File(ApplicationContextHolder.context.filesDir, CRASH_LOG_NAME)
            } else {
                null
            }

    actual fun read(): String? = file?.takeIf { it.exists() }?.readText()

    actual fun append(text: String) {
        file?.appendText(text)
    }

    actual fun clear() {
        file?.delete()
    }
}

internal actual fun installPlatformCrashHandler(
    onCrash: (Throwable, threadName: String) -> Unit
) {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        runCatching { onCrash(throwable, thread.name) }
        // Chaining is not optional: swallowing this would break Crashlytics,
        // Sentry, and the platform's own crash dialog.
        previous?.uncaughtException(thread, throwable)
    }
}

internal actual fun currentThreadName(): String = Thread.currentThread().name

internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()

internal actual fun randomId(): String = UUID.randomUUID().toString()

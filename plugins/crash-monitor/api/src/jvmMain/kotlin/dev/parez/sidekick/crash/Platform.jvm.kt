package dev.parez.sidekick.crash

import java.io.File
import java.util.UUID

internal actual object CrashStorage {
    private val file: File by lazy { File(System.getProperty("java.io.tmpdir"), CRASH_LOG_NAME) }

    actual fun read(): String? = file.takeIf { it.exists() }?.readText()

    actual fun append(text: String) {
        file.appendText(text)
    }

    actual fun clear() {
        file.delete()
    }
}

internal actual fun installPlatformCrashHandler(onCrash: (Throwable, threadName: String) -> Unit) {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        runCatching { onCrash(throwable, thread.name) }
        previous?.uncaughtException(thread, throwable)
    }
}

internal actual fun currentThreadName(): String = Thread.currentThread().name

internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()

internal actual fun randomId(): String = UUID.randomUUID().toString()

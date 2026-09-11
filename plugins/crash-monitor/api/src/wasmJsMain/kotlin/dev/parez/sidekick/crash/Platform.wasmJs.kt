package dev.parez.sidekick.crash

import kotlinx.browser.localStorage
import kotlinx.browser.window

/**
 * Browsers have no filesystem, so the crash log lives in `localStorage` — which is exactly the
 * durability this needs: it survives a reload, which is the browser's equivalent of a restart.
 */
internal actual object CrashStorage {
    actual fun read(): String? = runCatching { localStorage.getItem(CRASH_LOG_NAME) }.getOrNull()

    actual fun append(text: String) {
        runCatching { localStorage.setItem(CRASH_LOG_NAME, (read() ?: "") + text) }
    }

    actual fun clear() {
        runCatching { localStorage.removeItem(CRASH_LOG_NAME) }
    }
}

/**
 * Both hooks are needed: `onerror` catches synchronous throws, `onunhandledrejection` catches
 * failed promises — which on Kotlin/JS is where a crashing coroutine ends up. Wiring only the first
 * misses most real failures in an app that does any async work.
 */
internal actual fun installPlatformCrashHandler(onCrash: (Throwable, threadName: String) -> Unit) {
    window.addEventListener("error") { event ->
        runCatching { onCrash(BrowserError(event.toString()), "main") }
    }
    window.addEventListener("unhandledrejection") { event ->
        runCatching { onCrash(BrowserError(event.toString()), "promise") }
    }
}

/**
 * The DOM gives us an event, not a Throwable. Wrapping keeps the store's contract uniform; the
 * stack trace is whatever the browser put in the message.
 */
private class BrowserError(message: String) : Throwable(message)

internal actual fun currentThreadName(): String = "main"

// js(...) must be the entire body of a top-level function on wasmJs, hence the
// indirection — same shape as the monitors' TimeMillis.wasmJs.kt.
private fun dateNow(): Double = js("Date.now()")

internal actual fun currentTimeMillis(): Long = dateNow().toLong()

// No UUID API in the browser standard library that is safe across all targets;
// timestamp + counter is unique enough for a crash list.
private var idCounter = 0

internal actual fun randomId(): String = "${currentTimeMillis()}-${idCounter++}"

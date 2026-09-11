package dev.parez.sidekick.crash

import dev.parez.sidekick.crash.di.CrashMonitorKoinContext

/**
 * Captures crashes so they can be read *after* the restart that followed them.
 *
 * A crash you can only see with a debugger attached is a crash you have already failed to
 * reproduce. Install once during app startup:
 * ```kotlin
 * CrashMonitor.install(appPackagePrefix = "com.acme.app")
 * ```
 *
 * [appPackagePrefix] decides which stack frames are marked as the app's own, which is what makes a
 * trace readable at a glance. Omit it and every frame is treated as library code.
 */
public object CrashMonitor {

    /**
     * Installs the platform's uncaught-exception hook and loads any crashes written by a previous
     * run. Calling more than once is a no-op.
     */
    public fun install(appPackagePrefix: String? = null) {
        val store = CrashMonitorKoinContext.getDefaultStore()
        store.install(appPackagePrefix)
    }

    /** Records a handled exception — something you caught but still want to see later. */
    public fun recordNonFatal(throwable: Throwable, origin: String = "app") {
        CrashMonitorKoinContext.getDefaultStore().recordNonFatal(throwable, origin)
    }

    /** Drops every stored crash, in memory and on disk. */
    public fun clear() {
        CrashMonitorKoinContext.getDefaultStore().clear()
    }
}

/**
 * Installs the platform's "something escaped" hook.
 *
 * [onCrash] must be invoked synchronously — the process may not survive the call returning.
 */
internal expect fun installPlatformCrashHandler(onCrash: (Throwable, threadName: String) -> Unit)

/** Best-effort current thread name; platforms without threads report a sensible constant. */
internal expect fun currentThreadName(): String

internal expect fun currentTimeMillis(): Long

internal expect fun randomId(): String

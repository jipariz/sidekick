package dev.parez.sidekick.crash

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Holds captured crashes.
 *
 * Unlike the network and log monitors this store uses **no database**. At crash time the process is
 * being torn down — coroutines, Room and the invalidation tracker are all unsafe to reach for — so
 * the fatal path is a synchronous append to a plain file. Crash volumes are small enough
 * ([MAX_CRASHES]) to read whole at startup and keep in memory, which also means this plugin works
 * on every target including web, where the monitors fall back to in-memory anyway.
 */
public class CrashMonitorStore {

    private val _crashes = MutableStateFlow<List<CrashRecord>>(emptyList())
    public val crashes: StateFlow<List<CrashRecord>> = _crashes.asStateFlow()

    private var appPackagePrefix: String? = null
    private var installed = false

    internal fun install(appPackagePrefix: String?) {
        if (installed) return
        installed = true
        this.appPackagePrefix = appPackagePrefix

        // Read what the previous run left behind before the hook can append to it.
        _crashes.value =
            runCatching { CrashStorage.read()?.let(CrashCodec::decode) }
                .getOrNull()
                .orEmpty()
                .takeLast(MAX_CRASHES)

        installPlatformCrashHandler { throwable, threadName ->
            record(throwable, origin = "uncaught", fatal = true, threadName = threadName)
        }
    }

    public fun recordNonFatal(throwable: Throwable, origin: String) {
        record(throwable, origin = origin, fatal = false, threadName = currentThreadName())
    }

    public fun clear() {
        _crashes.value = emptyList()
        runCatching { CrashStorage.clear() }
    }

    private fun record(throwable: Throwable, origin: String, fatal: Boolean, threadName: String) {
        val record =
            CrashRecord(
                id = randomId(),
                timestamp = currentTimeMillis(),
                fatal = fatal,
                exceptionType = throwable::class.simpleName ?: "Throwable",
                message = throwable.message.orEmpty(),
                origin = origin,
                threadName = threadName,
                frames = throwable.toFrames(appPackagePrefix),
            )

        // Disk first: if this is a fatal crash the in-memory list is about to die with
        // the process, and the file is the only copy that outlives it.
        runCatching { CrashStorage.append(CrashCodec.encode(record)) }
        _crashes.update { (it + record).takeLast(MAX_CRASHES) }
    }
}

/**
 * Splits a stack trace into frames, marking the ones belonging to [appPackagePrefix].
 *
 * `stackTraceToString()` is the only multiplatform way to get a trace, but its shape is not
 * uniform: the JVM prefixes it with a `"<type>: <message>"` header line, Kotlin/Native does not.
 * Dropping the first line unconditionally therefore discarded a real frame on iOS — the top one,
 * which is the frame you most want. Drop it only when it is not itself a frame.
 */
internal fun Throwable.toFrames(appPackagePrefix: String?): List<StackFrame> =
    stackTraceToString()
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .dropWhile { !it.startsWith("at ") }
        .map { line ->
            StackFrame(
                text = line,
                isAppFrame = appPackagePrefix != null && line.contains(appPackagePrefix),
            )
        }
        .toList()

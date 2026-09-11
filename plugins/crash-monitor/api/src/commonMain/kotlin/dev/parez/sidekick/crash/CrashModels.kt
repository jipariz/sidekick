package dev.parez.sidekick.crash

import androidx.compose.runtime.Immutable

/**
 * One frame of a stack trace.
 *
 * [isAppFrame] is what makes a trace readable on a phone: the UI dims framework frames so the
 * handful of lines that belong to the app stand out. Derived from the `appPackagePrefix` passed to
 * [CrashMonitor.install].
 */
@Immutable public data class StackFrame(val text: String, val isAppFrame: Boolean)

/** A captured crash — fatal or handled. */
@Immutable
public data class CrashRecord(
    val id: String,
    val timestamp: Long,
    val fatal: Boolean,
    val exceptionType: String,
    val message: String,
    val origin: String,
    val threadName: String,
    val frames: List<StackFrame>,
) {
    /** Headline shown in the crash list. */
    public val title: String
        get() = if (message.isBlank()) exceptionType else "$exceptionType: $message"
}

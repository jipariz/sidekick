package dev.parez.sidekick.crash

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import dev.parez.sidekick.plugin.SidekickBadged
import dev.parez.sidekick.plugin.SidekickPlugin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Release-variant stubs for the Crash Monitor.
 *
 * Same fully-qualified names and signatures as the real family, so consumer code compiles unchanged
 * — but no uncaught-exception hook is installed and nothing is written to disk. Note that this
 * means the host app's own crash reporter keeps the handler entirely to itself.
 */
@Immutable public data class StackFrame(val text: String, val isAppFrame: Boolean)

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
    public val title: String
        get() = if (message.isBlank()) exceptionType else "$exceptionType: $message"
}

public class CrashMonitorStore {
    private val _crashes = MutableStateFlow<List<CrashRecord>>(emptyList())
    public val crashes: StateFlow<List<CrashRecord>> = _crashes.asStateFlow()

    @Suppress("UNUSED_PARAMETER")
    public fun recordNonFatal(throwable: Throwable, origin: String): Unit = Unit

    public fun clear(): Unit = Unit
}

public object CrashMonitor {
    @Suppress("UNUSED_PARAMETER") public fun install(appPackagePrefix: String? = null): Unit = Unit

    @Suppress("UNUSED_PARAMETER")
    public fun recordNonFatal(throwable: Throwable, origin: String = "app"): Unit = Unit

    public fun clear(): Unit = Unit
}

/** Release-variant stub — renders nothing and never badges. */
public class CrashMonitorPlugin : SidekickPlugin, SidekickBadged {
    override val badge: State<Int?> = mutableStateOf(null)

    override val id: String = "sidekick.crash-monitor"
    override val title: String = "Crashes"
    override val icon: ImageVector = Icons.Default.ReportProblem

    @Composable override fun Content(): Unit = Unit
}

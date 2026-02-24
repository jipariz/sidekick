package dev.parez.sidekick.gesture

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

private val monotonicClock = TimeSource.Monotonic

fun Modifier.fiveTapDetector(onFiveTap: () -> Unit): Modifier {
    var count = 0
    var lastTapMark = monotonicClock.markNow()
    return pointerInput(onFiveTap) {
        detectTapGestures {
            val now = monotonicClock.markNow()
            if ((now - lastTapMark) > 1.seconds) count = 0
            lastTapMark = now
            count++
            if (count >= 5) {
                count = 0
                onFiveTap()
            }
        }
    }
}

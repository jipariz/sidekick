package dev.parez.sidekick.demo.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TeamRocketRed = Color(0xFFE53935)
private val TeamRocketDeepRed = Color(0xFF8B0000)

/**
 * Error screen styled after Team Rocket's "blasting off again" moment. Used as a fallback when
 * fetching from the network fails — surfaces the underlying [message] in muted text and exposes a
 * [onRetry] button so the caller doesn't have to leave the screen.
 */
@Composable
fun TeamRocketError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            TeamRocketEmblem(modifier = Modifier.size(120.dp))
            Text(
                text = "Blasting off again!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = TeamRocketDeepRed,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Team Rocket intercepted that request.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onRetry,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = TeamRocketRed,
                        contentColor = Color.White,
                    ),
            ) {
                Text("Try Again")
            }
        }
    }
}

/**
 * The Team Rocket "R" emblem — a white-ringed red disc with a bold italic R. Gently wobbles to
 * suggest the balloon-getting-popped vibe.
 */
@Composable
private fun TeamRocketEmblem(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "team-rocket-emblem")
    val tilt by
        transition.animateFloat(
            initialValue = -6f,
            targetValue = 6f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "tilt",
        )
    Box(
        modifier =
            modifier
                .rotate(tilt)
                .background(TeamRocketRed, CircleShape)
                .border(width = 4.dp, color = Color.White, shape = CircleShape)
                .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "R",
            color = Color.White,
            fontSize = 72.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
        )
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────
// `@Preview` lives at `androidx.compose.ui.tooling.preview.Preview` — the CMP
// `org.jetbrains.compose.ui:ui-tooling-preview` artifact re-publishes the
// AndroidX class for non-JVM targets, so a single import works in commonMain.

@Preview
@Composable
private fun TeamRocketError_TimeoutPreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface { TeamRocketError(message = "Request timeout has expired", onRetry = {}) }
    }
}

@Preview
@Composable
private fun TeamRocketError_LongMessagePreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface {
            TeamRocketError(
                message =
                    "Failed to connect to https://pokeapi.co — the server " +
                        "returned 503 Service Unavailable after three retries. Check " +
                        "your connection and try again in a moment.",
                onRetry = {},
            )
        }
    }
}

@Preview
@Composable
private fun TeamRocketError_DarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface {
            TeamRocketError(message = "Unable to resolve host: api.pokemon.example", onRetry = {})
        }
    }
}

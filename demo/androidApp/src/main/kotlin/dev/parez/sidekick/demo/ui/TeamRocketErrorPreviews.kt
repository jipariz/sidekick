package dev.parez.sidekick.demo.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

// Mirrored in commonMain TeamRocketError.kt for in-IDE rendering; the copies
// here live in the demo:androidApp `application` module so Studio's "Run on
// Device" preview action can resolve an applicationId, package an APK, and
// deploy the preview to a connected device or emulator.

@Preview(showBackground = true)
@Composable
private fun TeamRocketError_TimeoutPreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface {
            TeamRocketError(
                message = "Request timeout has expired",
                onRetry = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TeamRocketError_LongMessagePreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface {
            TeamRocketError(
                message = "Failed to connect to https://pokeapi.co — the server " +
                    "returned 503 Service Unavailable after three retries. Check " +
                    "your connection and try again in a moment.",
                onRetry = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TeamRocketError_DarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface {
            TeamRocketError(
                message = "Unable to resolve host: api.pokemon.example",
                onRetry = {},
            )
        }
    }
}

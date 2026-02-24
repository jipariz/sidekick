package dev.parez.sidekick.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.parez.sidekick.SidekickShell

@Composable
fun DemoApp() {
    val plugin = remember { AppPreferencesPlugin() }
    val darkMode by plugin.accessor.darkMode.collectAsState()
    val apiUrl by plugin.accessor.apiUrl.collectAsState()

    MaterialTheme(
        colorScheme = if (darkMode) darkColorScheme() else lightColorScheme(),
    ) {
        SidekickShell(plugins = listOf(plugin)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = "Sidekick Demo", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "API URL: $apiUrl")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap 5 times anywhere to open Sidekick",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

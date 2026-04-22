package dev.parez.sidekick.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.parez.sidekick.SidekickState
import dev.parez.sidekick.plugin.PlatformInfo
import dev.parez.sidekick.plugin.SidekickAppInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SidekickMenu(
    state: SidekickState,
    appInfo: SidekickAppInfo?,
    title: String,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    val activePlugin = state.activePlugin

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        // ── Content — simple state-based routing with animated transitions ─
        AnimatedContent(
            targetState = activePlugin,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith
                            (slideOutHorizontally { it } + fadeOut())
                }
            },
        ) { plugin ->
            if (plugin != null) {
                SidekickPluginScreen(plugin, state)
            } else {
                SidekickPluginList(state, title, appInfo, navigationIcon, actions)
            }
        }

    }
}

// ── App info badge strip ──────────────────────────────────────────────────────

@Composable
internal fun AppInfoStrip(appInfo: SidekickAppInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (val p = appInfo.platform) {
            is PlatformInfo.Android -> {
                VersionBadge(p.appVersion, p.buildCode)
                BuildTypeBadge(p.buildType)
                InfoBadge("API ${p.apiLevel}")
                p.buildFlavor?.let { InfoBadge(it) }
                InfoBadge(p.deviceModel)
            }

            is PlatformInfo.Ios -> {
                VersionBadge(p.appVersion, p.buildCode)
                p.buildType?.let { BuildTypeBadge(it) }
                InfoBadge("iOS ${p.systemVersion}")
                InfoBadge(p.deviceModel)
            }

            is PlatformInfo.Desktop -> {
                InfoBadge(p.osName)
                InfoBadge("JVM ${p.jvmVersion}")
                InfoBadge("${p.availableProcessors} cores")
            }

            is PlatformInfo.Web -> {
                InfoBadge(p.browserName ?: "Browser")
            }

            PlatformInfo.Unknown -> Unit
        }

        // User-defined extras
        appInfo.extras.forEach { (key, value) ->
            InfoBadge("$key: $value")
        }
    }
}

// ── Badge helpers ─────────────────────────────────────────────────────────────

@Composable
private fun VersionBadge(version: String?, buildCode: Long?) {
    val label = buildString {
        version?.let { append("v").append(it) }
        buildCode?.let { code ->
            if (isNotEmpty()) append(" ")
            append("(").append(code).append(")")
        }
    }
    if (label.isNotEmpty()) InfoBadge(label)
}

@Composable
private fun BuildTypeBadge(buildType: String) {
    val (container, content) = when (buildType.lowercase()) {
        "debug" -> MaterialTheme.colorScheme.errorContainer to
                MaterialTheme.colorScheme.onErrorContainer

        "release" -> MaterialTheme.colorScheme.primaryContainer to
                MaterialTheme.colorScheme.onPrimaryContainer

        else -> MaterialTheme.colorScheme.secondaryContainer to
                MaterialTheme.colorScheme.onSecondaryContainer
    }
    InfoBadge(buildType, containerColor = container, contentColor = content)
}

@Composable
private fun InfoBadge(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}

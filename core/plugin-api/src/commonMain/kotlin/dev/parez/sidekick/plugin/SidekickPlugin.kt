package dev.parez.sidekick.plugin

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

interface SidekickPlugin {
    val id: String
    val title: String
    val icon: ImageVector
    @Composable fun Content()
}

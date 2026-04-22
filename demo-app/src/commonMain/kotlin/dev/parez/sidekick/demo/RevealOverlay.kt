package dev.parez.sidekick.demo

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.svenjacobs.reveal.RevealOverlayArrangement
import com.svenjacobs.reveal.RevealOverlayScope
import com.svenjacobs.reveal.shapes.balloon.Arrow
import com.svenjacobs.reveal.shapes.balloon.Balloon

@Composable
internal fun RevealOverlayScope.SidekickRevealOverlay(key: RevealKey?) {
    when (key) {
        RevealKey.SidekickFab -> FabTooltip(
            modifier = Modifier.align(horizontalArrangement = RevealOverlayArrangement.Start),
        )
        null -> {}
    }
}

@Composable
private fun FabTooltip(modifier: Modifier = Modifier) {
    Balloon(
        modifier = modifier.padding(8.dp),
        arrow = Arrow.end(),
        backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
        elevation = 2.dp,
    ) {
        Text(
            modifier = Modifier.padding(8.dp),
            text = "Tap here to open Sidekick",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            textAlign = TextAlign.Center,
        )
    }
}

package dev.parez.sidekick.demo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.parez.sidekick.demo.PokemonDetail
import dev.parez.sidekick.demo.PokemonRepository
import dev.parez.sidekick.demo.artworkUrlFor
import dev.parez.sidekick.demo.statDisplayName
import dev.parez.sidekick.demo.toDisplayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonDetailScreen(
    id: Int,
    name: String,
    repository: PokemonRepository,
    onBack: () -> Unit,
) {
    var detail by remember { mutableStateOf<PokemonDetail?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        runCatching { repository.getDetail(id) }
            .onSuccess { detail = it }
            .onFailure { error = it.message ?: "Unknown error" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            name.toDisplayName(),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "#${id.toString().padStart(3, '0')}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { padding ->
        when {
            error != null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    error!!,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp),
                )
            }
            detail == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            else -> DetailContent(detail = detail!!, modifier = Modifier.padding(padding))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(detail: PokemonDetail, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Hero artwork ──────────────────────────────────────────────────────
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            AsyncImage(
                model = artworkUrlFor(detail.id),
                contentDescription = detail.name,
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp),
            )
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))

            // ── Type chips ────────────────────────────────────────────────────
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                detail.types.sortedBy { it.slot }.forEach { slot ->
                    TypeChip(slot.type.name)
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))

            // ── Physical stats ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                PhysicalStatItem(
                    label = "Height",
                    value = "${detail.height / 10}.${(detail.height % 10)} m",
                )
                PhysicalStatItem(
                    label = "Weight",
                    value = "${detail.weight / 10}.${detail.weight % 10} kg",
                )
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))

            // ── Base stats ────────────────────────────────────────────────────
            SectionHeader("Base Stats")
            Spacer(Modifier.height(10.dp))
            detail.stats.forEach { stat ->
                StatBar(
                    name = statDisplayName(stat.stat.name),
                    value = stat.baseStat,
                    maxValue = 255,
                )
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))

            // ── Abilities ─────────────────────────────────────────────────────
            SectionHeader("Abilities")
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                detail.abilities.forEach { slot ->
                    AbilityChip(name = slot.ability.name, isHidden = slot.isHidden)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun PhysicalStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatBar(name: String, value: Int, maxValue: Int) {
    val fraction = (value.toFloat() / maxValue).coerceIn(0f, 1f)
    val barColor = when {
        fraction < 0.33f -> MaterialTheme.colorScheme.error
        fraction < 0.66f -> MaterialTheme.colorScheme.tertiary
        else             -> MaterialTheme.colorScheme.primary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(56.dp),
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = value.toString().padStart(3),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {}
            Surface(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxSize(),
                color = barColor,
            ) {}
        }
    }
}

@Composable
private fun TypeChip(typeName: String) {
    Surface(
        color = typeColor(typeName),
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = typeName.replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun AbilityChip(name: String, isHidden: Boolean) {
    Surface(
        color = if (isHidden)
            MaterialTheme.colorScheme.surfaceVariant
        else
            MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = if (isHidden) "${name.toDisplayName()} (hidden)" else name.toDisplayName(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (isHidden)
                MaterialTheme.colorScheme.onSurfaceVariant
            else
                MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

// ── Pokémon type colors (standard community palette) ─────────────────────────

private fun typeColor(type: String): Color = when (type.lowercase()) {
    "normal"   -> Color(0xFFA8A878)
    "fire"     -> Color(0xFFF08030)
    "water"    -> Color(0xFF6890F0)
    "electric" -> Color(0xFFF8D030)
    "grass"    -> Color(0xFF78C850)
    "ice"      -> Color(0xFF98D8D8)
    "fighting" -> Color(0xFFC03028)
    "poison"   -> Color(0xFFA040A0)
    "ground"   -> Color(0xFFE0C068)
    "flying"   -> Color(0xFFA890F0)
    "psychic"  -> Color(0xFFF85888)
    "bug"      -> Color(0xFFA8B820)
    "rock"     -> Color(0xFFB8A038)
    "ghost"    -> Color(0xFF705898)
    "dragon"   -> Color(0xFF7038F8)
    "dark"     -> Color(0xFF705848)
    "steel"    -> Color(0xFFB8B8D0)
    "fairy"    -> Color(0xFFEE99AC)
    else       -> Color(0xFF68A090)
}

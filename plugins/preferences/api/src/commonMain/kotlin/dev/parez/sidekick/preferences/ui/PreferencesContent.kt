package dev.parez.sidekick.preferences.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.parez.sidekick.preferences.BooleanPref
import dev.parez.sidekick.preferences.DoublePref
import dev.parez.sidekick.preferences.EnumPref
import dev.parez.sidekick.preferences.FloatPref
import dev.parez.sidekick.preferences.IntPref
import dev.parez.sidekick.preferences.LongPref
import dev.parez.sidekick.preferences.PreferenceDefinition
import dev.parez.sidekick.preferences.StringPref
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PreferencesContent(
    definitions: List<PreferenceDefinition<*>>,
    valueFlows: Map<String, StateFlow<Any>>,
    onSet: suspend (key: String, value: Any) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "App Preferences",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        StaggeredLayout(
            definitions = definitions,
            valueFlows = valueFlows,
            onSet = onSet,
            modifier = Modifier.padding(paddingValues),
        )
    }
}

// ── Staggered grid layout ─────────────────────────────────────────────────────

@Composable
private fun StaggeredLayout(
    definitions: List<PreferenceDefinition<*>>,
    valueFlows: Map<String, StateFlow<Any>>,
    onSet: suspend (String, Any) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    if (definitions.isEmpty()) {
        PreferencesEmptyState()
        return
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 280.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalItemSpacing = 12.dp,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(definitions, key = { it.key }) { def ->
            val flow = valueFlows[def.key] ?: return@items
            val value by flow.collectAsState()
            PreferenceCard(
                def = def,
                value = value,
                onChange = { newValue -> scope.launch { onSet(def.key, newValue) } },
            )
        }
    }
}

@Composable
private fun PreferenceCard(
    def: PreferenceDefinition<*>,
    value: Any,
    onChange: (Any) -> Unit,
) {
    val cardClick: (() -> Unit)? = if (def is BooleanPref) {
        { onChange(!(value as Boolean)) }
    } else null

    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TypeBadge(def)
                Text(
                    text = def.label,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            if (def.description.isNotEmpty()) {
                Text(
                    text = def.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            PreferenceCardEditor(def = def, value = value, onChange = onChange)
        }
    }

    if (cardClick != null) {
        Surface(
            onClick = cardClick,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
            content = content,
        )
    } else {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
            content = content,
        )
    }
}

@Composable
private fun PreferenceCardEditor(
    def: PreferenceDefinition<*>,
    value: Any,
    onChange: (Any) -> Unit,
) {
    when (def) {
        is BooleanPref -> {
            val checked = value as Boolean
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (checked) "Enabled" else "Disabled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = checked, onCheckedChange = null)
            }
        }
        is StringPref -> StringEditor(value = value as String, onChange = { onChange(it) })
        is IntPref    -> NumberEditor(value = value.toString(), onSave = { it.toIntOrNull()?.let(onChange) })
        is LongPref   -> NumberEditor(value = value.toString(), onSave = { it.toLongOrNull()?.let(onChange) })
        is FloatPref  -> NumberEditor(value = value.toString(), onSave = { it.toFloatOrNull()?.let(onChange) })
        is DoublePref -> NumberEditor(value = value.toString(), onSave = { it.toDoubleOrNull()?.let(onChange) })
        is EnumPref -> EnumEditor(
            options = def.options,
            currentValue = (value as? Enum<*>)?.name ?: value.toString(),
            onChange = onChange,
        )
        else          -> ReadOnlyEditor(value = value.toString())
    }
}

// ── Shared editors ────────────────────────────────────────────────────────────

@Composable
private fun StringEditor(value: String, onChange: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            label = { Text("Value") },
        )
        Button(
            onClick = { onChange(text) },
            enabled = text != value,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Save")
        }
    }
}

@Composable
private fun NumberEditor(value: String, onSave: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = MaterialTheme.shapes.small,
            label = { Text("Value") },
        )
        Button(
            onClick = { onSave(text) },
            enabled = text != value,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Save")
        }
    }
}

@Composable
private fun ReadOnlyEditor(value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EnumEditor(options: List<String>, currentValue: String, onChange: (Any) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            InputChip(
                selected = currentValue == option,
                onClick = { onChange(option) },
                label = {
                    Text(
                        text = option.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
            )
        }
    }
}

// ── Type badge ────────────────────────────────────────────────────────────────

@Composable
private fun TypeBadge(def: PreferenceDefinition<*>, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (def) {
        is BooleanPref -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, "BOOL")
        is StringPref  -> Triple(MaterialTheme.colorScheme.primaryContainer,   MaterialTheme.colorScheme.onPrimaryContainer,   "STR")
        is IntPref     -> Triple(MaterialTheme.colorScheme.tertiaryContainer,  MaterialTheme.colorScheme.onTertiaryContainer,  "INT")
        is LongPref    -> Triple(MaterialTheme.colorScheme.tertiaryContainer,  MaterialTheme.colorScheme.onTertiaryContainer,  "LONG")
        is FloatPref   -> Triple(MaterialTheme.colorScheme.tertiaryContainer,  MaterialTheme.colorScheme.onTertiaryContainer,  "FLOAT")
        is DoublePref  -> Triple(MaterialTheme.colorScheme.tertiaryContainer,  MaterialTheme.colorScheme.onTertiaryContainer,  "DOUBLE")
        is EnumPref    -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, "ENUM")
        else           -> Triple(MaterialTheme.colorScheme.surfaceVariant,     MaterialTheme.colorScheme.onSurfaceVariant,     "VAL")
    }
    Surface(color = bg, shape = MaterialTheme.shapes.extraSmall, modifier = modifier) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun parseNumber(def: PreferenceDefinition<*>, text: String): Any? = when (def) {
    is IntPref    -> text.toIntOrNull()
    is LongPref   -> text.toLongOrNull()
    is FloatPref  -> text.toFloatOrNull()
    is DoublePref -> text.toDoubleOrNull()
    else          -> null
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun PreferencesEmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
                text = "No preferences defined",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Add @SidekickPreference annotations to expose settings here",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
            )
        }
    }
}

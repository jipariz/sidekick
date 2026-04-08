package dev.parez.sidekick.preferences.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.parez.sidekick.preferences.BooleanPref
import dev.parez.sidekick.preferences.DoublePref
import dev.parez.sidekick.preferences.FloatPref
import dev.parez.sidekick.preferences.IntPref
import dev.parez.sidekick.preferences.LongPref
import dev.parez.sidekick.preferences.PreferenceDefinition
import dev.parez.sidekick.preferences.StringPref
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ── Adaptive breakpoints ──────────────────────────────────────────────────────

private val CompactBreakpoint = 600.dp
private val ExpandedBreakpoint = 840.dp

/**
 * Root composable for the Preferences plugin.
 *
 * - **< 600 dp (compact/mobile)** — Single-column list with inline editors.
 * - **600–840 dp (medium/tablet)** — 2-column card grid; all preferences visible at once.
 * - **≥ 840 dp (expanded/desktop/web)** — 3-column card grid; all preferences visible at once.
 */
@Composable
internal fun PreferencesContent(
    definitions: List<PreferenceDefinition<*>>,
    valueFlows: Map<String, StateFlow<Any>>,
    onSet: suspend (key: String, value: Any) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        when {
            maxWidth >= ExpandedBreakpoint -> GridLayout(
                definitions = definitions,
                valueFlows = valueFlows,
                onSet = onSet,
                columns = 3,
            )
            maxWidth >= CompactBreakpoint -> GridLayout(
                definitions = definitions,
                valueFlows = valueFlows,
                onSet = onSet,
                columns = 2,
            )
            else -> CompactLayout(
                definitions = definitions,
                valueFlows = valueFlows,
                onSet = onSet,
            )
        }
    }
}

// ── Compact layout (<600 dp) ──────────────────────────────────────────────────

@Composable
private fun CompactLayout(
    definitions: List<PreferenceDefinition<*>>,
    valueFlows: Map<String, StateFlow<Any>>,
    onSet: suspend (String, Any) -> Unit,
) {
    val scope = rememberCoroutineScope()

    if (definitions.isEmpty()) {
        PreferencesEmptyState()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        items(definitions, key = { it.key }) { def ->
            val flow = valueFlows[def.key] ?: return@items
            val value by flow.collectAsState()
            CompactPreferenceItem(
                def = def,
                value = value,
                onChange = { newValue -> scope.launch { onSet(def.key, newValue) } },
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun CompactPreferenceItem(
    def: PreferenceDefinition<*>,
    value: Any,
    onChange: (Any) -> Unit,
) {
    when (def) {
        is BooleanPref -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChange(!(value as Boolean)) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TypeBadge(def)
                Column(modifier = Modifier.weight(1f)) {
                    Text(def.label, style = MaterialTheme.typography.titleSmall)
                    if (def.description.isNotEmpty()) {
                        Text(
                            def.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                Switch(
                    checked = value as Boolean,
                    onCheckedChange = { onChange(it) },
                )
            }
        }

        is StringPref -> {
            var text by remember(value) { mutableStateOf(value as String) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TypeBadge(def)
                    Text(def.label, style = MaterialTheme.typography.titleSmall)
                }
                if (def.description.isNotEmpty()) {
                    Text(
                        def.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                )
                Button(
                    onClick = { onChange(text) },
                    enabled = text != value,
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save")
                }
            }
        }

        is IntPref, is LongPref, is FloatPref, is DoublePref -> {
            var text by remember(value) { mutableStateOf(value.toString()) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TypeBadge(def)
                    Text(def.label, style = MaterialTheme.typography.titleSmall)
                }
                if (def.description.isNotEmpty()) {
                    Text(
                        def.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.small,
                )
                Button(
                    onClick = { parseNumber(def, text)?.let { onChange(it) } },
                    enabled = parseNumber(def, text) != null && text != value.toString(),
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save")
                }
            }
        }

        else -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TypeBadge(def)
                Text(def.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(
                    value.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Grid layout (≥600 dp) ─────────────────────────────────────────────────────

@Composable
private fun GridLayout(
    definitions: List<PreferenceDefinition<*>>,
    valueFlows: Map<String, StateFlow<Any>>,
    onSet: suspend (String, Any) -> Unit,
    columns: Int,
) {
    val scope = rememberCoroutineScope()

    if (definitions.isEmpty()) {
        PreferencesEmptyState()
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChange(!checked) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (checked) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                    contentDescription = null,
                    tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = if (checked) "Enabled" else "Disabled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = checked, onCheckedChange = { onChange(it) })
            }
        }
        is StringPref -> StringEditor(value = value as String, onChange = { onChange(it) })
        is IntPref    -> NumberEditor(value = value.toString(), onSave = { it.toIntOrNull()?.let(onChange) })
        is LongPref   -> NumberEditor(value = value.toString(), onSave = { it.toLongOrNull()?.let(onChange) })
        is FloatPref  -> NumberEditor(value = value.toString(), onSave = { it.toFloatOrNull()?.let(onChange) })
        is DoublePref -> NumberEditor(value = value.toString(), onSave = { it.toDoubleOrNull()?.let(onChange) })
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

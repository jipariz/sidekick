package dev.parez.sidekick.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import dev.parez.sidekick.plugin.SidekickPlugin
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class PreferencesPlugin(
    private val pluginTitle: String,
    val definitions: List<PreferenceDefinition<*>>,
    val valueFlows: Map<String, StateFlow<Any>>,
    val onSet: suspend (key: String, value: Any) -> Unit,
) : SidekickPlugin {
    override val id: String = "sidekick.preferences.${pluginTitle.lowercase().replace(" ", "_")}"
    override val title: String = pluginTitle
    override val icon = Icons.Default.Settings

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(definitions) { definition ->
                val flow = valueFlows[definition.key] ?: return@items
                val value by flow.collectAsState()
                PreferenceItem(
                    definition = definition,
                    value = value,
                    onValueChange = { newValue ->
                        scope.launch { onSet(definition.key, newValue) }
                    },
                )
            }
        }
    }
}

@Composable
private fun PreferenceItem(
    definition: PreferenceDefinition<*>,
    value: Any,
    onValueChange: (Any) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        when (definition) {
            is BooleanPref -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(definition.label)
                        if (definition.description.isNotEmpty()) {
                            Text(definition.description)
                        }
                    }
                    Switch(
                        checked = value as Boolean,
                        onCheckedChange = { onValueChange(it) },
                    )
                }
            }
            is StringPref -> {
                var text by remember(value) { mutableStateOf(value as String) }
                Text(definition.label)
                if (definition.description.isNotEmpty()) {
                    Text(definition.description)
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = { onValueChange(text) }) {
                    Text("Save")
                }
            }
            is IntPref -> {
                var text by remember(value) { mutableStateOf(value.toString()) }
                Text(definition.label)
                if (definition.description.isNotEmpty()) {
                    Text(definition.description)
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = { text.toIntOrNull()?.let { onValueChange(it) } }) {
                    Text("Save")
                }
            }
            else -> {
                Text(definition.label)
                Text(value.toString())
            }
        }
    }
}

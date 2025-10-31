package dev.parez.sidekick.ksp.model

data class PreferenceProperty(
    val name: String,
    val type: String,                       // simple type name, e.g. "Boolean", "ColorTheme"
    val qualifiedType: String?,             // fully qualified, e.g. "dev.parez.sidekick.demo.ColorTheme"; null for primitives
    val isEnum: Boolean,
    val enumValues: List<String>,           // enum entry names, empty for non-enums
    val defaultValue: String,
    val label: String,
    val description: String,
)

package dev.parez.sidekick.preferences.ksp.model

/**
 * `defaultValue` is a *Kotlin-source-form* literal: e.g. `false`, `42`, `0.5f`, `"hi"`, or the enum
 * entry name (without the type prefix) like `DEFAULT`. The generators emit it verbatim into the
 * generated Kotlin, so it must already be valid Kotlin for the property's type — see
 * DefaultExtractor for the parsing rules.
 */
data class PreferenceProperty(
    val name: String,
    val type: String, // simple type name, e.g. "Boolean", "ColorTheme"
    val qualifiedType:
        String?, // fully qualified, e.g. "dev.parez.sidekick.demo.ColorTheme"; null for primitives
    val isEnum: Boolean,
    val enumValues: List<String>, // enum entry names, empty for non-enums
    val defaultValue: String,
    val label: String,
    val description: String,
)

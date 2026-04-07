package dev.parez.sidekick.ksp.model

data class PreferenceProperty(
    val name: String,
    val type: String,
    val defaultValue: String,
    val label: String,
    val description: String,
)

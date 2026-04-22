package dev.parez.sidekick.preferences

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class SidekickPreferences(
    val title: String = "",
    val storeName: String = "",
)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class Preference(
    val label: String = "",
    val description: String = "",
    val defaultValue: String = "",
)

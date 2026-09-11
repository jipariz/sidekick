package dev.parez.sidekick.preferences

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class SidekickPreferences(val title: String = "", val storeName: String = "")

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
public annotation class Preference(val label: String = "", val description: String = "")

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
public annotation class IgnorePreference

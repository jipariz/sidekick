package dev.parez.sidekick.preferences

public actual fun createPreferenceStore(storeName: String): PreferenceStore =
    InMemoryPreferenceStore()

package dev.parez.sidekick.preferences

actual fun createPreferenceStore(storeName: String): PreferenceStore = InMemoryPreferenceStore()

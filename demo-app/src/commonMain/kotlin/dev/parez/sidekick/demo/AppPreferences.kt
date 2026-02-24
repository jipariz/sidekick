package dev.parez.sidekick.demo

import dev.parez.sidekick.preferences.Preference
import dev.parez.sidekick.preferences.SidekickPreferences

@SidekickPreferences(title = "App Preferences")
class AppPreferences {
    @Preference(label = "Dark Mode", defaultValue = "false")
    var darkMode: Boolean = false

    @Preference(label = "Is nice", defaultValue = "false")
    var isNice: Boolean = false

    @Preference(label = "API URL", defaultValue = "https://api.example.com")
    var apiUrl: String = ""

    @Preference(label = "Timeout (s)", defaultValue = "30")
    var timeout: Int = 0
}

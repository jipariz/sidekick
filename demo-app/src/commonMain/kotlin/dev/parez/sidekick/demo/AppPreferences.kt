package dev.parez.sidekick.demo

import dev.parez.sidekick.preferences.Preference
import dev.parez.sidekick.preferences.SidekickPreferences

enum class ColorTheme {
    DYNAMIC, DEFAULT, FIRE, WATER, GRASS, ELECTRIC, PSYCHIC
}

@SidekickPreferences(title = "Preferences")
class AppPreferences {
    @Preference(label = "Dark Mode", defaultValue = "false")
    var darkMode: Boolean = false

    @Preference(label = "Color Theme", defaultValue = "DEFAULT")
    var colorTheme: ColorTheme = ColorTheme.DEFAULT

    @Preference(label = "Show Pokédex Numbers", defaultValue = "true")
    var showNumbers: Boolean = true

    @Preference(label = "Shiny Sprites", defaultValue = "false")
    var shinySprites: Boolean = false
}

package dev.parez.sidekick.demo

import dev.parez.sidekick.preferences.Preference
import dev.parez.sidekick.preferences.SidekickPreferences

enum class ColorTheme {
    DYNAMIC,
    DEFAULT,
    FIRE,
    WATER,
    GRASS,
    ELECTRIC,
    PSYCHIC,
}

@SidekickPreferences(title = "Preferences")
class AppPreferences {
    var darkMode: Boolean = false

    var colorTheme: ColorTheme = ColorTheme.DEFAULT

    @Preference(label = "Show Pokédex Numbers") var showNumbers: Boolean = true

    var shinySprites: Boolean = false
}

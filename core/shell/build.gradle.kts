plugins { id("sidekick.kmp.library") }

kotlin {
    androidLibrary { namespace = "dev.parez.sidekick.shell" }
    sourceSets {
        commonMain.dependencies {
            api(projects.core.pluginApi)
            // api so that consumers — which call Sidekick(...) and typically use
            // Icons.Default.* in their own FAB / debug UI — transitively get
            // the extended icons artifact without declaring it themselves.
            api(libs.compose.material.iconsExtended)
            implementation(libs.kotlinx.coroutinesCore)
            // KMP BackHandler — wires Android system/gesture back into the
            // overlay's plugin-detail screen; a true no-op on other targets.
            implementation(libs.compose.ui.backhandler)
            // Drag-to-reorder for the plugin menu grid.
            implementation(libs.reorderable)
        }
        androidMain.dependencies {
            // DataStore-backed persistence for the menu's plugin order on
            // Android. Other targets use the platform-native key/value store
            // (NSUserDefaults / java.util.prefs / localStorage) via the
            // MenuOrderStore expect/actual.
            implementation(libs.androidx.datastore.prefs)
        }
    }
}

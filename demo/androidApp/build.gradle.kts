plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "dev.parez.sidekick.demo.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.parez.sidekick.demo"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(projects.demo.shared)
    // shared re-exports the SDK types via the KMP library's androidMain, but AGP
    // applications don't pull transitive Compose / activity deps from a KMP
    // library's compileClasspath the same way. Add the Activity / Compose /
    // Sidekick plugin-api deps directly here for the MainActivity.
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(projects.core.pluginApi)
    // @Preview annotation + on-device renderer for Studio's "Run on Device"
    // preview action. `ui-tooling-preview` exposes the annotation; `ui-tooling`
    // is debug-only and ships the runtime needed to actually render previews
    // when deployed.
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}

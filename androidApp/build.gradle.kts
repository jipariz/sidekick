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
    implementation(projects.composeApp)
    // composeApp re-exports the shell + plugin-api types via the KMP library's
    // androidMain, but AGP applications don't pull transitive Compose / activity
    // deps from a KMP library's compileClasspath the same way. Add the Activity
    // / Compose / Sidekick plugin-api deps directly here for the MainActivity.
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(projects.core.pluginApi)
}

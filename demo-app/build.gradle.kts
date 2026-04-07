import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    jvm()
    js {
        browser()
        binaries.executable()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin"))
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(projects.sidekickPreferencesApi)
                implementation(projects.sidekickDebug)
            }
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(projects.sidekickDebug)
        }
        jsMain.dependencies {
            implementation(projects.sidekickDebug)
        }
        wasmJsMain.dependencies {
            implementation(projects.sidekickDebug)
        }
    }
}

dependencies {
    debugImplementation(projects.sidekickDebug)
    releaseImplementation(projects.sidekickNoop)
    add("kspCommonMainMetadata", projects.sidekickPreferencesKsp)
}

// All Kotlin compilation and KSP tasks must wait for the common metadata KSP pass
tasks.matching { task ->
    task.name != "kspCommonMainKotlinMetadata" &&
        (task.name.startsWith("compile") && task.name.contains("Kotlin") ||
            task.name.startsWith("ksp"))
}.configureEach {
    dependsOn("kspCommonMainKotlinMetadata")
}
// The KSP common-main task's outputs are declared as a source-set srcDir, which the
// Gradle build cache doesn't restore reliably.  Disable caching and always re-check.
tasks.matching { it.name == "kspCommonMainKotlinMetadata" }.configureEach {
    outputs.cacheIf { false }
    val outDir = layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin")
    outputs.upToDateWhen { outDir.get().asFile.exists() }
}

android {
    namespace = "dev.parez.sidekick.demo"
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

compose.desktop {
    application {
        mainClass = "dev.parez.sidekick.demo.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "dev.parez.sidekick.demo"
            packageVersion = "1.0.0"
        }
    }
}

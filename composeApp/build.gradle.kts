// AGP 9 migration notes (also documented in CLAUDE.md → Debug vs Release):
//
//  - This is a KMP library via `com.android.kotlin.multiplatform.library`. The
//    `:composeApp:` Gradle path produces the shared code; the thin Android shell
//    lives in `:androidApp:` because AGP 9 forbids the legacy `com.android.application`
//    + `kotlin.multiplatform` combo in one subproject.
//  - The Android `debugImplementation` / `releaseImplementation` noop swap is gone:
//    AGP 9's KotlinMultiplatformAndroidLibraryExtension does not expose
//    `publishLibraryVariants("release", "debug")`. Consumers needing release-stripped
//    builds should mirror the property-gated recipe in CLAUDE.md. This demo is
//    dev-only and uses the real shell + monitor modules unconditionally.
//  - Web entry point lives in jsMain AND wasmJsMain (duplicated 12-line `main.kt`),
//    not in a `webMain` intermediate — under Kotlin 2.3.20 + the standard hierarchy
//    template, `webMain` doesn't expose `kotlinx.browser`.
//  - The `nonIosMain` intermediate is wired manually (`sourceSets { … dependsOn(nonIosMain) }`)
//    because `withAndroidTarget()` in `applyDefaultHierarchyTemplate` does not match
//    AGP 9's `KotlinMultiplatformAndroidLibraryTarget`.
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("dev.parez.sidekick.preferences")
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.room3)
}

kotlin {
    androidLibrary {
        namespace = "dev.parez.sidekick.demo"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
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
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    // Room 3.0.0-alpha05 still doesn't publish iOS variants. Carve out a
    // `nonIosMain` intermediate source set that all non-iOS leaves inherit
    // from, and put the Pokemon Room cache there; iOS gets an in-memory
    // fallback (see composeApp/src/iosMain/kotlin/.../db/InMemoryPokemonCache.kt).
    //
    // Under AGP 9's `com.android.kotlin.multiplatform.library`, the Android
    // target is no longer a `KotlinAndroidTarget` — `withAndroidTarget()` does
    // not match it. Build the nonIos hierarchy manually so it covers the new
    // Android KMP library target too.
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting
        val nonIosMain by creating { dependsOn(commonMain) }
        named("androidMain") { dependsOn(nonIosMain) }
        named("jvmMain") { dependsOn(nonIosMain) }
        named("jsMain") { dependsOn(nonIosMain) }
        named("wasmJsMain") { dependsOn(nonIosMain) }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.materialIconsExtended)
                implementation(projects.plugins.preferences.api)
                implementation(projects.plugins.customScreen.api)
                // The demo is dev-only — all targets pull the real shell + monitor
                // modules directly. Prod consumers swap real for noop using the
                // property-gated recipe in docs/release-builds.md (AGP 9 no longer
                // supports the debugImplementation/releaseImplementation variant
                // pattern Sidekick relied on before).
                implementation(projects.core.shell)
                implementation(projects.plugins.networkMonitor.ui)
                implementation(projects.plugins.networkMonitor.ktor)
                implementation(projects.plugins.logMonitor.ui)
                implementation(projects.plugins.logMonitor.kermit)
                implementation(libs.kermit)
                implementation(libs.ktor.client.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.contentNegotiation)
                implementation(libs.ktor.serialization.kotlinxJson)
                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor)
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.compose.adaptive)
                implementation(libs.compose.adaptive.layout)
                implementation(libs.compose.adaptive.navigation)
                implementation(libs.compose.material3.adaptive.navigation.suite)
                implementation(libs.reveal.core)
                implementation(libs.reveal.shapes)
            }
        }
        val nonIosMain by getting {
            dependencies {
                // Room 3 alpha03 — only the targets in the nonIos group resolve.
                implementation(libs.room3.runtime)
            }
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqlite.bundled)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.ktor.client.cio)
            implementation(libs.sqlite.bundled)
        }
        jsMain {
            kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/js/jsMain/kotlin"))
        }
        jsMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation(libs.sqlite.web)
            implementation(npm("sql.js", "1.10.3"))
            implementation(npm("@cashapp/sqldelight-sqljs-worker", "2.1.0"))
            implementation(devNpm("copy-webpack-plugin", "9.1.0"))
            implementation(
                npm("sqlite-wasm-worker", layout.projectDirectory.dir("sqlite-worker").asFile)
            )
        }
        wasmJsMain {
            kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/wasmJs/wasmJsMain/kotlin"))
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation(libs.sqlite.web)
            implementation(
                npm("sqlite-wasm-worker", layout.projectDirectory.dir("sqlite-worker").asFile)
            )
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", projects.plugins.preferences.ksp)
    // Room 3 KSP — only on the targets Room supports.
    add("kspAndroid", libs.room3.compiler)
    add("kspJvm", libs.room3.compiler)
    add("kspJs", libs.room3.compiler)
    add("kspWasmJs", libs.room3.compiler)
}

room3 {
    schemaDirectory("$projectDir/schemas")
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

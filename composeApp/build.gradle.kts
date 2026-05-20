// FIXME(AGP 9 migration WIP) — known build issues on this branch:
//
//  1. compileNonIosMainKotlinMetadata fails because Room 3.0.0-alpha03's per-target
//     KSP output (`build/generated/ksp/<target>/...`) isn't visible to the
//     intermediate `nonIosMain` metadata compile under Kotlin 2.3.20 + Gradle 9.4 +
//     AGP 9.2.1. Same shape as the preferences plugin issue patched in
//     plugins/preferences/gradle-plugin: per-target KSP generated code does not
//     surface to commonMain/intermediate metadata passes. Likely needs a similar
//     "move semantics" / consolidated-stableDir pattern, or a Room version bump.
//
//  2. compileWebMainKotlinMetadata can't resolve `kotlinx.browser` in
//     src/webMain/kotlin/dev/parez/sidekick/demo/main.kt. `webMain` is the standard
//     intermediate between commonMain and (jsMain + wasmJsMain) — under Kotlin
//     2.3.20 the stdlib parts that contain `kotlinx.browser` apparently aren't
//     reachable from webMain. Workaround options: move main.kt back to both jsMain
//     and wasmJsMain, or add the right kotlin-browser dep on webMain.
//
//  3. The Android `debugImplementation` / `releaseImplementation` noop swap is
//     gone: AGP 9's KotlinMultiplatformAndroidLibraryExtension does not expose
//     `publishLibraryVariants("release", "debug")` (see commit 1's body). All
//     targets in this composeApp pull the real shell + monitor modules
//     unconditionally. Consumers needing release-stripped builds should mirror
//     the property-gated recipe documented in CLAUDE.md for non-Android targets.
//
// commit 1 of this branch (AGP 9 library-side migration) is the load-bearing
// change; this commit is the demo-app split scaffolding that AGP 9 also requires.
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

    // Room 3.0.0-alpha03 does not yet publish iOS variants. Carve out a
    // `nonIosMain` intermediate source set that all non-iOS leaves inherit
    // from, and put the Pokemon Room cache there; iOS gets an in-memory
    // fallback (see composeApp/src/iosMain/kotlin/.../db/InMemoryPokemonCache.kt).
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("nonIos") {
                withAndroidTarget()
                withJvm()
                withJs()
                withWasmJs()
            }
        }
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

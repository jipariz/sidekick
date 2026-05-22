// Demo "shared" module — KMP library that powers every app shell in this
// directory (androidApp, desktopApp, webApp, iosApp). Entry points and
// platform-specific runtime deps (Skiko's currentOs jar, sql.js NPM, etc.)
// live in the respective app modules; shared only contains cross-platform
// code plus per-target expect/actual implementations.
//
// AGP 9 migration notes (also in CLAUDE.md → Debug vs Release):
//  - This is a KMP library via `com.android.kotlin.multiplatform.library`.
//  - The Android `debugImplementation` / `releaseImplementation` noop swap
//    is gone in AGP 9. The demo here pulls the real shell + monitor modules
//    unconditionally — consumers needing release-stripped builds should
//    follow the property-gated recipe in CLAUDE.md.
//  - The `nonIosMain` intermediate is wired manually because AGP 9's
//    `KotlinMultiplatformAndroidLibraryTarget` is not matched by
//    `withAndroidTarget()` in the default hierarchy template.
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
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate()

    // PokemonDatabase + Room's generated PokemonDatabaseConstructor use
    // `expect`/`actual` classes. The classes-in-Beta warning is informational.
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        val commonMain by getting
        // Two orthogonal intermediates collapse the per-target carve-outs:
        //   nonIosMain  — has Room (Room 3 doesn't publish for iOS yet)
        //   nonWebMain  — holds the no-op BrowserHistoryEffect actual
        // android/jvm see both; ios sees only nonWebMain; web sees only nonIosMain.
        //
        // `webMain` is auto-created by applyDefaultHierarchyTemplate() and is
        // already the parent of jsMain + wasmJsMain — we just hook it under
        // nonIosMain so Room (declared on nonIosMain) keeps flowing into web.
        val nonIosMain by creating { dependsOn(commonMain) }
        val nonWebMain by creating { dependsOn(commonMain) }
        named("androidMain") { dependsOn(nonIosMain); dependsOn(nonWebMain) }
        named("jvmMain") { dependsOn(nonIosMain); dependsOn(nonWebMain) }
        named("iosMain") { dependsOn(nonWebMain) }
        named("webMain") { dependsOn(nonIosMain) }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.material.iconsExtended)
                implementation(projects.plugins.preferences.api)
                implementation(projects.plugins.customScreen.api)
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
                implementation(libs.compose.adaptive.navigation3)
                implementation(libs.compose.material3.adaptive.navigation.suite)
                implementation(libs.navigation3.runtime)
                // `@Preview` lives in commonMain since CMP 1.5; promote the
                // tooling-preview dep out of androidMain so previews show in
                // common files like `TeamRocketError`.
                implementation(libs.compose.ui.tooling.preview)
                // Room 3 publishes KMP variants for every target we build
                // (Android, JVM, iosArm64, iosSimulatorArm64, js, wasmJs) as of
                // 3.0.0-alpha05 — previously iOS was carved out via nonIosMain.
                implementation(libs.room3.runtime)
                // Force-bump over the 1.0.1 (Kotlin 2.2.20 abi) that
                // adaptive-navigation3:1.3.0-beta01 transitively pulls.
                implementation(libs.navigationevent.compose)
                implementation(libs.reveal.core)
                implementation(libs.reveal.shapes)
            }
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqlite.bundled)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.sqlite.bundled)
        }
        val webMain by getting {
            dependencies {
                // Wires androidx.navigation3 backstack ↔ browser History API.
                implementation(libs.navigation3.browser)
                // The adaptive-navigation3 1.3.0-beta01 graph still pulls
                // Kotlin-2.2-era (abi 2.2.0) AOSP klibs (lifecycle, savedstate,
                // navigationevent). Those reference `externrefToBoolean`, a
                // Wasm internal that Kotlin 2.3 no longer exports — the page
                // throws "function import requires a callable" at instantiation.
                // Force every offending artifact up to its abi-2.3 release.
                implementation(libs.androidx.lifecycle.runtime.aosp)
                implementation(libs.androidx.lifecycle.common.aosp)
                implementation(libs.androidx.lifecycle.runtimeCompose.aosp)
                implementation(libs.androidx.savedstate.aosp)
                implementation(libs.androidx.savedstate.compose.aosp)
                implementation(libs.androidx.navigationevent.aosp)
                implementation(libs.androidx.navigationevent.compose.aosp)
            }
        }
        jsMain {
            kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/js/jsMain/kotlin"))
        }
        jsMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation(libs.sqlite.web)
        }
        wasmJsMain {
            kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/wasmJs/wasmJsMain/kotlin"))
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation(libs.sqlite.web)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            // Bundled SQLite ships per-target binaries for iosArm64 and
            // iosSimulatorArm64 — same artifact android/jvm use.
            implementation(libs.sqlite.bundled)
        }
        // KMP KSP doesn't auto-register generated dirs for native source sets,
        // mirroring the js/wasmJs pattern above.
        val iosArm64Main by getting {
            kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/iosArm64/iosArm64Main/kotlin"))
        }
        val iosSimulatorArm64Main by getting {
            kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/iosSimulatorArm64/iosSimulatorArm64Main/kotlin"))
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", projects.plugins.preferences.ksp)
    add("kspAndroid", libs.room3.compiler)
    add("kspJvm", libs.room3.compiler)
    add("kspJs", libs.room3.compiler)
    add("kspWasmJs", libs.room3.compiler)
    add("kspIosArm64", libs.room3.compiler)
    add("kspIosSimulatorArm64", libs.room3.compiler)
    // AGP 9's `com.android.kotlin.multiplatform.library` doesn't expose a
    // `debugImplementation` configuration, so `ui-tooling` (the renderer for
    // `@Preview` in commonMain) is attached to the Android runtime classpath
    // instead — the wiring recommended in the official KMP docs:
    // https://kotlinlang.org/docs/multiplatform/compose-previews.html
    add("androidRuntimeClasspath", libs.compose.ui.tooling)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

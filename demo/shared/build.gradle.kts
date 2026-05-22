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
        val nonIosMain by creating { dependsOn(commonMain) }
        named("androidMain") { dependsOn(nonIosMain) }
        named("jvmMain") { dependsOn(nonIosMain) }
        named("jsMain") { dependsOn(nonIosMain) }
        named("wasmJsMain") { dependsOn(nonIosMain) }
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
                implementation(libs.compose.material3.adaptive.navigation.suite)
                implementation(libs.reveal.core)
                implementation(libs.reveal.shapes)
            }
        }
        val nonIosMain by getting {
            dependencies {
                // Room 3 only publishes for the nonIos targets.
                implementation(libs.room3.runtime)
            }
        }
        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqlite.bundled)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.sqlite.bundled)
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
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", projects.plugins.preferences.ksp)
    add("kspAndroid", libs.room3.compiler)
    add("kspJvm", libs.room3.compiler)
    add("kspJs", libs.room3.compiler)
    add("kspWasmJs", libs.room3.compiler)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("dev.parez.sidekick.preferences")
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.room3)
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
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    // Room 3.0.0-alpha03 does not yet publish iOS variants. Carve out a
    // `nonIosMain` intermediate source set that all non-iOS leaves inherit
    // from, and put the Pokemon Room cache there; iOS gets an in-memory
    // fallback (see demo-app/src/iosMain/kotlin/.../db/InMemoryPokemonCache.kt).
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
                // compileOnly: same rationale as `core/shell` below — give
                // commonMain the type stubs (`NetworkMonitorPlugin`,
                // `LogMonitorPlugin`, `installSidekick`, `LogMonitorLogWriter`,
                // …) without putting the real plugins on Android release's
                // runtime classpath, where they would collide with the noop
                // variant. Per-platform source sets add the real modules via
                // `implementation` below; Android picks real (debug) or noop
                // (release) via the variant swap in the `dependencies { }`
                // block further down.
                compileOnly(projects.plugins.networkMonitor.ui)
                compileOnly(projects.plugins.networkMonitor.ktor)
                compileOnly(projects.plugins.logMonitor.ui)
                compileOnly(projects.plugins.logMonitor.kermit)
                implementation(libs.kermit)
                implementation(libs.ktor.client.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.contentNegotiation)
                implementation(libs.ktor.serialization.kotlinxJson)
                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor)
                // compileOnly: provides the Sidekick() / SidekickState type stubs
                // to commonMain. Per-platform source sets add the shell jar
                // via implementation below; Android picks shell (debug) or noop
                // (release) via the variant swap further down. Without `compileOnly`,
                // shell ends up on Android release's runtime classpath next to noop
                // and AGP fails the build (`checkReleaseDuplicateClasses` / dex merger).
                compileOnly(projects.core.shell)
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
            implementation(projects.core.shell)
            implementation(projects.plugins.networkMonitor.ui)
            implementation(projects.plugins.networkMonitor.ktor)
            implementation(projects.plugins.logMonitor.ui)
            implementation(projects.plugins.logMonitor.kermit)
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
            implementation(projects.core.shell)
            implementation(projects.plugins.networkMonitor.ui)
            implementation(projects.plugins.networkMonitor.ktor)
            implementation(projects.plugins.logMonitor.ui)
            implementation(projects.plugins.logMonitor.kermit)
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
            implementation(projects.core.shell)
            implementation(projects.plugins.networkMonitor.ui)
            implementation(projects.plugins.networkMonitor.ktor)
            implementation(projects.plugins.logMonitor.ui)
            implementation(projects.plugins.logMonitor.kermit)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(projects.core.shell)
            implementation(projects.plugins.networkMonitor.ui)
            implementation(projects.plugins.networkMonitor.ktor)
            implementation(projects.plugins.logMonitor.ui)
            implementation(projects.plugins.logMonitor.kermit)
        }
    }
}

dependencies {
    debugImplementation(projects.core.shell)
    releaseImplementation(projects.core.noop)
    // Monitor plugins: debug Android keeps the real recording trio (api + plugin
    // + ktor/kermit). Release Android swaps the whole family for the noop
    // module, which exposes the same FQNs but with empty bodies — no SQLDelight
    // DB opens, no HTTP/log entries persist.
    debugImplementation(projects.plugins.networkMonitor.ui)
    debugImplementation(projects.plugins.networkMonitor.ktor)
    releaseImplementation(projects.plugins.networkMonitor.noop)
    debugImplementation(projects.plugins.logMonitor.ui)
    debugImplementation(projects.plugins.logMonitor.kermit)
    releaseImplementation(projects.plugins.logMonitor.noop)
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

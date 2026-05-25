plugins {
    id("sidekick.kmp.library")
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
}

kotlin {
    androidLibrary { namespace = "dev.parez.sidekick.network" }

    // Room's `@ConstructedBy(...)` companion is an `expect object`. The
    // classes-in-Beta warning is informational; demo/shared opts in the same way.
    compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutinesCore)
            // `room3-runtime` and the per-target `sqlite-bundled` drivers are
            // referenced by our compiled per-target actuals — consumers need
            // them on their runtime classpath, so they ship as `api` to flow
            // transitively through the published POMs.
            api(libs.room3.runtime)
            api(libs.koin.core)
            api(libs.androidx.paging.common)
        }
        androidMain.dependencies {
            // ApplicationContextHolder lives in core:plugin-api androidMain
            implementation(projects.core.pluginApi)
            api(libs.sqlite.bundled)
            implementation(libs.koin.android)
        }
        iosMain.dependencies { api(libs.sqlite.bundled) }
        jvmMain.dependencies { api(libs.sqlite.bundled) }
        // Web targets use Room 3 + sqlite-web's WebWorkerSQLiteDriver. The
        // driver delegates SQL to a consumer-supplied web worker — see the
        // "Consumer setup: web persistence" section in CLAUDE.md. If the
        // consumer hasn't bundled a worker, the store's init probe fails
        // gracefully and falls back to the in-memory list.
        jsMain.dependencies { api(libs.sqlite.web) }
        wasmJsMain.dependencies { api(libs.sqlite.web) }

        // KMP KSP doesn't auto-register generated dirs for native source sets.
        // jsMain/wasmJsMain entries are kept in step with demo/shared so future
        // worker-backed web persistence (if added) compiles without re-wiring.
        jsMain { kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/js/jsMain/kotlin")) }
        wasmJsMain {
            kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/wasmJs/wasmJsMain/kotlin"))
        }
        val iosArm64Main by getting {
            kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/iosArm64/iosArm64Main/kotlin"))
        }
        val iosSimulatorArm64Main by getting {
            kotlin.srcDir(
                layout.buildDirectory.dir(
                    "generated/ksp/iosSimulatorArm64/iosSimulatorArm64Main/kotlin"
                )
            )
        }
    }
}

dependencies {
    add("kspAndroid", libs.room3.compiler)
    add("kspJvm", libs.room3.compiler)
    add("kspJs", libs.room3.compiler)
    add("kspWasmJs", libs.room3.compiler)
    add("kspIosArm64", libs.room3.compiler)
    add("kspIosSimulatorArm64", libs.room3.compiler)
}

room3 { schemaDirectory("$projectDir/schemas") }

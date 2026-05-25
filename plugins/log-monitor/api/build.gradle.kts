plugins {
    id("sidekick.kmp.library")
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
}

kotlin {
    androidLibrary { namespace = "dev.parez.sidekick.log" }

    compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutinesCore)
            // See note in network-monitor:api — these run on the consumer's
            // runtime classpath, so they're `api` to propagate through POMs.
            api(libs.room3.runtime)
            api(libs.koin.core)
            api(libs.androidx.paging.common)
        }
        androidMain.dependencies {
            implementation(projects.core.pluginApi)
            api(libs.sqlite.bundled)
            implementation(libs.koin.android)
        }
        iosMain.dependencies { api(libs.sqlite.bundled) }
        jvmMain.dependencies { api(libs.sqlite.bundled) }
        // Web targets use Room + sqlite-web's WebWorkerSQLiteDriver — see the
        // matching note in network-monitor:api/build.gradle.kts.
        jsMain.dependencies { api(libs.sqlite.web) }
        wasmJsMain.dependencies { api(libs.sqlite.web) }

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

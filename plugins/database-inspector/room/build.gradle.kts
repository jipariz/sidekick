plugins { id("sidekick.kmp.library") }

kotlin {
    androidLibrary { namespace = "dev.parez.sidekick.database.room" }

    sourceSets {
        val commonMain by getting
        // The collector lives in nonWebMain, not commonMain, for two reasons:
        //   1. Web has no working implementation — see attachRoomDatabase.web.kt.
        //   2. It calls Room's pooled-connection API, whose SQLiteStatement lambda
        //      types are not on the common metadata compile classpath, so the same
        //      code in commonMain fails to resolve `statement.step()`.
        // `webMain` comes from applyDefaultHierarchyTemplate; nonWebMain must be
        // wired by hand because that template does not match AGP 9's Android target.
        val nonWebMain by creating { dependsOn(commonMain) }
        // Hooked to the concrete target source sets rather than the `iosMain`
        // intermediate: that intermediate is materialised lazily by the hierarchy
        // template and is not yet present while this script is evaluated.
        named("androidMain") { dependsOn(nonWebMain) }
        named("jvmMain") { dependsOn(nonWebMain) }
        named("iosArm64Main") { dependsOn(nonWebMain) }
        named("iosSimulatorArm64Main") { dependsOn(nonWebMain) }

        commonMain.dependencies {
            api(projects.plugins.databaseInspector.api)
            implementation(libs.kotlinx.coroutinesCore)
            // `api` rather than compileOnly: this module exists to talk to Room, and
            // it matches how network-monitor:api and log-monitor:api already
            // propagate it. Consumers of this module have a Room database by
            // definition.
            api(libs.room3.runtime)
        }
    }
}

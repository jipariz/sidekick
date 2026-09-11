plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.vanniktechMavenPublish) apply false
    // ktfmt — Kotlin formatter applied to every subproject below.
    alias(libs.plugins.ktfmt) apply false
    // detekt — static analysis, likewise applied per-subproject below.
    alias(libs.plugins.detekt) apply false
    // metalava — API signature tracking, applied by sidekick.kmp.library. Declared
    // here so the plugin's classes are on the build classpath for build-logic.
    alias(libs.plugins.metalava) apply false
    // Registers `updateModuleVersions` + `checkModuleVersions` tasks.
    id("sidekick.version.update")
}

val artifactIdMap = mapOf(
    ":core:plugin-api" to "plugin-api",
    ":core:shell" to "shell",
    ":core:noop" to "noop",
    ":plugins:preferences:api" to "preferences",
    ":plugins:preferences:ksp" to "preferences-ksp",
    ":plugins:network-monitor:api" to "network-monitor",
    ":plugins:network-monitor:ui" to "network-monitor-ui",
    ":plugins:network-monitor:ktor" to "network-monitor-ktor",
    ":plugins:network-monitor:noop" to "network-monitor-noop",
    ":plugins:log-monitor:api" to "log-monitor",
    ":plugins:log-monitor:ui" to "log-monitor-ui",
    ":plugins:log-monitor:kermit" to "log-monitor-kermit",
    ":plugins:log-monitor:noop" to "log-monitor-noop",
    ":plugins:custom-screen:api" to "custom-screen",
    ":plugins:database-inspector:api" to "database-inspector",
    ":plugins:database-inspector:ui" to "database-inspector-ui",
    ":plugins:database-inspector:room" to "database-inspector-room",
    ":plugins:database-inspector:noop" to "database-inspector-noop",
    ":plugins:crash-monitor:api" to "crash-monitor",
    ":plugins:crash-monitor:ui" to "crash-monitor-ui",
    ":plugins:crash-monitor:noop" to "crash-monitor-noop",
)

subprojects {
    if (artifactIdMap.containsKey(path)) {
        ext.set("sidekick.artifactId", artifactIdMap[path])
    }

    // Apply ktfmt to every subproject — it auto-detects Kotlin source sets
    // (commonMain/androidMain/iosMain/etc. for KMP modules, plus main/test
    // for the BOM and KSP modules). Run `./gradlew ktfmtFormat` to format,
    // `./gradlew ktfmtCheck` to verify (the latter is CI-friendly).
    apply(plugin = rootProject.libs.plugins.ktfmt.get().pluginId)
    extensions.configure<com.ncorti.ktfmt.gradle.KtfmtExtension> {
        // Kotlinlang style: 4-space indent, max line 100. Matches the
        // existing codebase conventions; switch to googleStyle() if we
        // ever migrate to 2-space.
        kotlinLangStyle()
    }

    // detekt covers what ktfmt and explicit-API mode cannot see: Compose-specific
    // smells (missing/misplaced modifiers, ViewModel forwarding, mutable params)
    // plus a narrow set of correctness rules. Config in config/detekt.yml.
    apply(plugin = rootProject.libs.plugins.detekt.get().pluginId)
    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        parallel = true
        buildUponDefaultConfig = true
        config.setFrom(rootProject.file("config/detekt.yml"))
        // Always point at the path — detekt tolerates a missing file, and
        // `detektBaseline` needs somewhere to write. Baselined entries are
        // pre-existing findings to burn down, not permanent exemptions.
        baseline = project.file("detekt-baseline.xml")
        // Type resolution is off: detekt 1.23 embeds the Kotlin 1.9 frontend and
        // this repo is on Kotlin 2.4, so a typed pass cannot resolve our sources.
        // The syntactic rules we care about here do not need it.
    }
    dependencies {
        add("detektPlugins", rootProject.libs.detekt.composeRules)
    }
    // KMP modules have no single "main" source set, so both the analysis task and
    // the baseline-generation task need to be pointed at the whole src tree —
    // they are separate task types and do not share configuration.
    tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
        setSource(files("src"))
        include("**/*.kt")
        exclude("**/build/**", "**/resources/**")
    }
    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        setSource(files("src"))
        include("**/*.kt")
        exclude("**/build/**", "**/resources/**")
        reports {
            html.required.set(false)
            sarif.required.set(false)
            md.required.set(false)
            txt.required.set(false)
            xml.required.set(false)
        }
    }
}
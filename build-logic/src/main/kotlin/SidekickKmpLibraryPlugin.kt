import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.gradle.api.artifacts.VersionCatalogsExtension

class SidekickKmpLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        // AGP 9+ replacement for the old `com.android.library` + `kotlin.multiplatform`
        // combo, which is no longer compatible per AGP 9.0 release notes. The new
        // plugin extends KotlinMultiplatformExtension with an `android { … }`
        // block (was `androidLibrary { … }` before AGP 8.12.0, deprecated since
        // AGP 9.1.0-alpha09) that consolidates compileSdk / minSdk / namespace.
        pluginManager.apply("com.android.kotlin.multiplatform.library")
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        // vanniktech.maven.publish brings in `maven-publish` + `signing` and adds
        // a tighter coordinates/POM DSL plus a one-shot Central upload task.
        pluginManager.apply("com.vanniktech.maven.publish")
        // Reads this module's version.properties and sets project.version.
        // The BOM later picks up each module's version via project.version,
        // so this must run before the coordinates() call below.
        pluginManager.apply("sidekick.version.read")

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        val compileSdkVersion = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
        val minSdkVersion = libs.findVersion("android-minSdk").get().requiredVersion.toInt()

        extensions.configure<KotlinMultiplatformExtension> {
            // The Android KMP target extension is added to KotlinMultiplatformExtension by
            // the `com.android.kotlin.multiplatform.library` plugin. AGP 8.12.0 introduced
            // the `android { … }` accessor as a replacement for the older `androidLibrary { … }`,
            // and `androidLibrary` was marked deprecated in AGP 9.1.0-alpha09. We stick
            // with `androidLibrary` for now because in build scripts that also apply third-
            // party plugins (historically `app.cash.sqldelight`), the new `android { … }`
            // accessor can be shadowed by KMP's deprecated
            // `android(name: String, …): KotlinAndroidTarget` member function — Gradle's
            // static accessor generator picks the member over the runtime extension.
            // `androidLibrary { … }` is unambiguous in both cases.
            (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryExtension>("androidLibrary") {
                this.compileSdk = compileSdkVersion
                this.minSdk = minSdkVersion
                // NOTE: AGP 9.x KotlinMultiplatformAndroidLibraryExtension does NOT
                // support `publishLibraryVariants("release", "debug")` — that was the
                // legacy `com.android.library` + `kotlin.multiplatform` combo. The new
                // KMP library plugin publishes a single Android variant. Consumers must
                // pick real-vs-noop via a property-gated dependency swap (mirroring the
                // non-Android targets pattern documented in CLAUDE.md), not
                // debugImplementation/releaseImplementation. This is a documented public
                // API change as of the AGP 9 migration.
            }
            iosArm64()
            iosSimulatorArm64()
            jvm()
            js {
                browser()
            }
            @OptIn(ExperimentalWasmDsl::class)
            wasmJs {
                browser()
            }

            // CMP 1.11.0 deprecated `compose.runtime` / `.foundation` / `.material3` / `.ui`
            // String accessors with "Specify dependency directly." — pull from the version
            // catalog instead.
            sourceSets.commonMain.dependencies {
                implementation(libs.findLibrary("compose-runtime").get())
                implementation(libs.findLibrary("compose-foundation").get())
                implementation(libs.findLibrary("compose-material3").get())
                implementation(libs.findLibrary("compose-ui").get())
            }

            // Sidekick uses `expect class` / `expect object` (Room database
            // constructors, SQLite drivers, …) by design. The classes-in-Beta
            // warning is informational, not a defect.
            compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }

        extensions.configure<MavenPublishBaseExtension> {
            // Coordinates: groupId is fixed, artifactId comes from the per-module
            // override set in the root build.gradle.kts ext("sidekick.artifactId"),
            // falling back to the Gradle project name. Version is set by
            // SidekickVersionReadConventionPlugin from this module's
            // version.properties — read it back via project.version.
            val resolvedArtifactId = (extensions.extraProperties
                .takeIf { it.has("sidekick.artifactId") }
                ?.get("sidekick.artifactId") as? String)
                ?: project.name
            coordinates(
                groupId = "dev.parez.sidekick",
                artifactId = resolvedArtifactId,
                version = project.version.toString(),
            )

            // POM metadata + Central Portal target + conditional signing.
            configureSidekickPublication(project)
        }
    }
}

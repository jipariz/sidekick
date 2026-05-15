import com.android.build.gradle.LibraryExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.gradle.api.artifacts.VersionCatalogsExtension

class SidekickKmpLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.library")
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        // vanniktech.maven.publish brings in `maven-publish` + `signing` and adds
        // a tighter coordinates/POM DSL plus a one-shot Central upload task.
        pluginManager.apply("com.vanniktech.maven.publish")

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        val compileSdkVersion = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
        val minSdkVersion = libs.findVersion("android-minSdk").get().requiredVersion.toInt()

        extensions.configure<LibraryExtension> {
            compileSdk = compileSdkVersion
            defaultConfig {
                minSdk = minSdkVersion
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }
        }

        val compose = extensions.getByType<ComposeExtension>().dependencies

        extensions.configure<KotlinMultiplatformExtension> {
            androidTarget {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
                publishLibraryVariants("release", "debug")
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

            sourceSets.commonMain.dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
            }
        }

        val sidekickVersion = findProperty("sidekick.version") as String

        extensions.configure<MavenPublishBaseExtension> {
            // Coordinates: groupId is fixed, artifactId comes from the per-module
            // override set in the root build.gradle.kts ext("sidekick.artifactId"),
            // falling back to the Gradle project name.
            val resolvedArtifactId = (extensions.extraProperties
                .takeIf { it.has("sidekick.artifactId") }
                ?.get("sidekick.artifactId") as? String)
                ?: project.name
            coordinates(
                groupId = "dev.parez.sidekick",
                artifactId = resolvedArtifactId,
                version = sidekickVersion,
            )

            // POM metadata + Central Portal target + conditional signing.
            configureSidekickPublication(project)
        }
    }
}

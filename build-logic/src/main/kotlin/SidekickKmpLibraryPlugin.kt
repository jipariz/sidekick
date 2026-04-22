import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
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
        pluginManager.apply("maven-publish")

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
            iosX64()
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

        afterEvaluate {
            val desiredArtifactId = extensions.extraProperties.takeIf {
                it.has("sidekick.artifactId")
            }?.get("sidekick.artifactId") as? String ?: return@afterEvaluate

            val sidekickVersion = findProperty("sidekick.version") as String

            extensions.configure<PublishingExtension> {
                publications.withType<MavenPublication>().configureEach {
                    groupId = "dev.parez.sidekick"
                    version = sidekickVersion
                    artifactId = artifactId.replaceFirst(project.name, desiredArtifactId)
                }
            }
        }
    }
}

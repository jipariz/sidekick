import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Project

/**
 * Shared POM metadata applied to every Sidekick publication. Maven Central
 * rejects publications missing any of name/description/url/licenses/scm/developers,
 * so we configure them all here and let each module override coordinates.
 *
 * Usage in a non-KMP module that uses `com.vanniktech.maven.publish` directly:
 * ```kotlin
 * mavenPublishing {
 *     coordinates("dev.parez.sidekick", "my-artifact", project.version.toString())
 *     configureSidekickPublication(project)
 * }
 * ```
 *
 * For KMP library modules, `SidekickKmpLibraryPlugin` already calls this.
 */
fun MavenPublishBaseExtension.configureSidekickPublication(project: Project) {
    publishToMavenCentral(automaticRelease = false)

    val hasSigningKey = project.providers.environmentVariable("ORG_GRADLE_PROJECT_signingInMemoryKey").isPresent ||
        project.providers.gradleProperty("signingInMemoryKey").isPresent
    if (hasSigningKey) {
        signAllPublications()
    }

    pom {
        name.set("Sidekick")
        description.set(
            "Kotlin Multiplatform debug overlay SDK with built-in plugins for network, " +
                "log, preferences, and custom debug screens. Compose-based, Android/iOS/JVM/Wasm/JS."
        )
        url.set("https://github.com/jipariz/sidekick")
        inceptionYear.set("2025")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("jipariz")
                name.set("Jiri Parizek")
                email.set("jiri.parizek@strv.com")
                url.set("https://github.com/jipariz")
            }
        }
        scm {
            url.set("https://github.com/jipariz/sidekick")
            connection.set("scm:git:https://github.com/jipariz/sidekick.git")
            developerConnection.set("scm:git:ssh://git@github.com/jipariz/sidekick.git")
        }
    }
}

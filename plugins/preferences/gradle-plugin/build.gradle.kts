import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.tasks.WriteProperties

plugins {
    alias(libs.plugins.kotlinJvm)
    `java-gradle-plugin`
    alias(libs.plugins.vanniktechMavenPublish)
}

// Set group/version at script-evaluation time, before `java-gradle-plugin`
// generates the plugin-marker publication — otherwise the marker captures
// project.version="unspecified" and a stale copy is published alongside the
// real one. Per Gradle's plugin-resolution convention, the marker keeps its
// own groupId (= plugin id), so we deliberately do NOT override it here.
// providers.gradleProperty (vs findProperty) traverses property providers
// correctly when this build is consumed as an included build from the root.
group = "dev.parez.sidekick"
version = providers.gradleProperty("sidekick.version").orElse("0.0.0-LOCAL").get()

fun Provider<PluginDependency>.toDep() = map {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}

dependencies {
    compileOnly(libs.plugins.kotlinMultiplatform.toDep())
    // KSP is bundled transitively so a consumer applying this plugin does NOT
    // need to declare id("com.google.devtools.ksp") themselves. The plugin
    // calls pluginManager.apply("com.google.devtools.ksp") at runtime — that
    // requires the KSP plugin to be on this plugin's runtime classpath, hence
    // implementation rather than compileOnly.
    implementation(libs.plugins.ksp.toDep())
}

val generateVersionProperties = tasks.register<WriteProperties>("generateVersionProperties") {
    destinationFile.set(layout.buildDirectory.file("generated-resources/sidekick-preferences.properties"))
    property("version", project.version.toString())
}

sourceSets {
    main {
        resources.srcDir(generateVersionProperties.map { it.destinationFile.get().asFile.parentFile })
    }
}

gradlePlugin {
    plugins {
        register("sidekickPreferences") {
            id = "dev.parez.sidekick.preferences"
            implementationClass = "dev.parez.sidekick.preferences.SidekickPreferencesPlugin"
        }
        register("sidekick") {
            id = "dev.parez.sidekick"
            implementationClass = "dev.parez.sidekick.SidekickGradlePlugin"
        }
    }
}

// Vanniktech bundles the main plugin jar + per-plugin marker publications
// and uploads them to the Sonatype Central Portal as a single deployment.
// The previous raw `maven-publish` + `publishing { repositories { mavenCentralPortal } }`
// setup tried to PUT each file individually, which the Portal's bundle-only
// upload API rejected with 404.
mavenPublishing {
    coordinates("dev.parez.sidekick", "sidekick-preferences-gradle-plugin", project.version.toString())
    configure(GradlePlugin(javadocJar = JavadocJar.Empty(), sourcesJar = true))
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)

    val hasSigningKey = providers.environmentVariable("ORG_GRADLE_PROJECT_signingInMemoryKey").isPresent ||
        providers.gradleProperty("signingInMemoryKey").isPresent
    if (hasSigningKey) {
        signAllPublications()
    }

    pom {
        name.set("Sidekick Preferences Gradle Plugin")
        description.set(
            "Gradle plugin that wires the Sidekick @SidekickPreferences KSP processor " +
                "into a Kotlin Multiplatform project."
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

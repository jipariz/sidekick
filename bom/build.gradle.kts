import com.vanniktech.maven.publish.JavaPlatform
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    `java-platform`
    alias(libs.plugins.vanniktechMavenPublish)
}

group = "dev.parez.sidekick"
// BOM is calendar-versioned. The per-module versions it pins are derived
// transitively from each `:projects.*` accessor's project.version.
version = findProperty("sidekick.bomVersion") as String

dependencies {
    constraints {
        api(projects.core.pluginApi)
        api(projects.core.shell)
        api(projects.core.noop)
        api(projects.plugins.preferences.api)
        api(projects.plugins.preferences.ksp)
        api(projects.plugins.networkMonitor.api)
        api(projects.plugins.networkMonitor.ui)
        api(projects.plugins.networkMonitor.ktor)
        api(projects.plugins.networkMonitor.noop)
        api(projects.plugins.logMonitor.api)
        api(projects.plugins.logMonitor.ui)
        api(projects.plugins.logMonitor.kermit)
        api(projects.plugins.logMonitor.noop)
        api(projects.plugins.customScreen.api)
    }
}

mavenPublishing {
    coordinates("dev.parez.sidekick", "bom", project.version.toString())
    configure(JavaPlatform())
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)

    val hasSigningKey = providers.environmentVariable("ORG_GRADLE_PROJECT_signingInMemoryKey").isPresent ||
        providers.gradleProperty("signingInMemoryKey").isPresent
    if (hasSigningKey) {
        signAllPublications()
    }

    pom {
        name.set("Sidekick BOM")
        description.set(
            "Bill of Materials for the Sidekick KMP debug overlay SDK. Calendar-versioned; " +
                "pins each plugin module's independent semver version for a coherent install."
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

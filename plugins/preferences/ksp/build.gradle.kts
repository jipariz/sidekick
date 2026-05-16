import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.vanniktechMavenPublish)
    id("sidekick.version.read")
}

group = "dev.parez.sidekick"
// version is set by sidekick.version.read from version.properties

dependencies {
    compileOnly(libs.ksp.api)
    implementation(libs.kotlinpoet.core)
    implementation(libs.kotlinpoet.ksp)
    compileOnly(projects.plugins.preferences.api)
}

mavenPublishing {
    coordinates("dev.parez.sidekick", "preferences-ksp", project.version.toString()) // version from sidekick.version.read
    configure(KotlinJvm(javadocJar = JavadocJar.Empty(), sourcesJar = true))
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)

    val hasSigningKey = providers.environmentVariable("ORG_GRADLE_PROJECT_signingInMemoryKey").isPresent ||
        providers.gradleProperty("signingInMemoryKey").isPresent
    if (hasSigningKey) {
        signAllPublications()
    }

    pom {
        name.set("Sidekick Preferences KSP Processor")
        description.set(
            "KSP processor that generates type-safe accessors and a SidekickPlugin " +
                "implementation from @SidekickPreferences-annotated classes."
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

import com.vanniktech.maven.publish.JavaPlatform
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    `java-platform`
    alias(libs.plugins.vanniktechMavenPublish)
}

group = "dev.parez.sidekick"
version = findProperty("sidekick.version") as String

dependencies {
    constraints {
        api("dev.parez.sidekick:plugin-api:$version")
        api("dev.parez.sidekick:runtime:$version")
        api("dev.parez.sidekick:noop:$version")
        api("dev.parez.sidekick:preferences:$version")
        api("dev.parez.sidekick:preferences-ksp:$version")
        api("dev.parez.sidekick:network-monitor:$version")
        api("dev.parez.sidekick:network-monitor-plugin:$version")
        api("dev.parez.sidekick:network-monitor-ktor:$version")
        api("dev.parez.sidekick:log-monitor:$version")
        api("dev.parez.sidekick:log-monitor-plugin:$version")
        api("dev.parez.sidekick:log-monitor-kermit:$version")
        api("dev.parez.sidekick:custom-screens:$version")
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
        description.set("Bill of Materials for the Sidekick KMP debug overlay SDK.")
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

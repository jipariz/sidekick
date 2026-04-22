import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.WriteProperties

plugins {
    alias(libs.plugins.kotlinJvm)
    `java-gradle-plugin`
    `maven-publish`
}

fun Provider<PluginDependency>.toDep() = map {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}

dependencies {
    compileOnly(libs.plugins.kotlinMultiplatform.toDep())
}

val sidekickVersionProvider = providers.gradleProperty("sidekick.version").orElse("unspecified")

val generateVersionProperties = tasks.register<WriteProperties>("generateVersionProperties") {
    destinationFile.set(layout.buildDirectory.file("generated-resources/sidekick-preferences.properties"))
    property("version", sidekickVersionProvider)
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
    }
}

afterEvaluate {
    extensions.configure<PublishingExtension> {
        publications.withType<MavenPublication>().configureEach {
            groupId = "dev.parez.sidekick"
            version = sidekickVersionProvider.get()
        }
    }
}

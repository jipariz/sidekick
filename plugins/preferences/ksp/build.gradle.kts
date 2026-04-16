plugins {
    alias(libs.plugins.kotlinJvm)
    `maven-publish`
}

dependencies {
    compileOnly(libs.ksp.api)
    implementation(libs.kotlinpoet.core)
    implementation(libs.kotlinpoet.ksp)
    compileOnly(projects.plugins.preferences.api)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "dev.parez.sidekick"
            artifactId = "preferences-ksp"
            version = findProperty("sidekick.version") as String
        }
    }
}

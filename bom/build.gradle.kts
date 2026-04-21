plugins {
    `java-platform`
    `maven-publish`
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

publishing {
    publications {
        create<MavenPublication>("bom") {
            from(components["javaPlatform"])
            artifactId = "bom"
        }
    }
}

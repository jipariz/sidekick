rootProject.name = "Sidekick"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":core:plugin-api")
include(":core:runtime")
include(":core:noop")
include(":plugins:preferences:api")
include(":plugins:preferences:ksp")
include(":plugins:network-monitor:api")
include(":plugins:network-monitor:plugin")
include(":plugins:network-monitor:ktor")
include(":plugins:log-monitor:api")
include(":plugins:log-monitor:plugin")
include(":plugins:log-monitor:kermit")
include(":plugins:custom-screens:api")
include(":demo-app")
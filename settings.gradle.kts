rootProject.name = "Sidekick"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    includeBuild("plugins/preferences/gradle-plugin")
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
include(":core:shell")
include(":core:noop")
include(":plugins:preferences:api")
include(":plugins:preferences:ksp")
include(":plugins:network-monitor:api")
include(":plugins:network-monitor:ui")
include(":plugins:network-monitor:ktor")
include(":plugins:network-monitor:noop")
include(":plugins:log-monitor:api")
include(":plugins:log-monitor:ui")
include(":plugins:log-monitor:kermit")
include(":plugins:log-monitor:noop")
include(":plugins:custom-screen:api")
include(":bom")
// AGP 9 dropped `com.android.application` + `kotlin.multiplatform` in the same
// subproject. The demo lives in two modules now: composeApp holds all KMP code
// (incl. its Android library target), androidApp is the thin .apk shell.
include(":composeApp")
include(":androidApp")
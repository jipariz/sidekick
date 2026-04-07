plugins {
    alias(libs.plugins.kotlinJvm)
}

dependencies {
    compileOnly(libs.ksp.api)
    implementation(libs.kotlinpoet.core)
    implementation(libs.kotlinpoet.ksp)
    compileOnly(projects.plugins.preferences.api)
}

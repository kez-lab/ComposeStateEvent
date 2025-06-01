plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":state-event-annotations"))
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
}

kotlin {
    jvmToolchain(17)
}
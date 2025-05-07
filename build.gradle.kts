// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false // This makes Kotlin available
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.parcelize) apply false // Ensure this is declared for availability
    // alias(libs.plugins.kotlin.serialization) apply false // Only if needed globally
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}

plugins {
    alias(libs.plugins.jetpack.ui.library)   // adds Android library + Compose config
    alias(libs.plugins.jetpack.dagger.hilt)  // applies Hilt + kapt correctly
    alias(libs.plugins.jetpack.dokka)
    // If your typed routes use @Serializable and it's not already added by ui.library:
    // alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "dev.atick.feature.records"
}

dependencies {
    // modules
    implementation(project(":core:ui"))
    implementation(project(":data"))

    // If your convention plugin doesn't already add these at feature level, keep them:
    // implementation(libs.androidx.navigation.compose)
    // implementation(libs.kotlinx.serialization.json)
}

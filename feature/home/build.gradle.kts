plugins {
    alias(libs.plugins.jetpack.ui.library)
    alias(libs.plugins.jetpack.dagger.hilt)
    alias(libs.plugins.jetpack.dokka)
}

android {
    namespace = "dev.atick.feature.home"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }
}

kotlin {
    // Use JDK 21 toolchain
    jvmToolchain(21)

    // Ensure Kotlin emits JVM bytecode for 21 too
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    // Modules
    implementation(project(":core:ui"))
    implementation(project(":data"))

    // AndroidX
    implementation("androidx.compose.material3:material3:1.2.1")

    // Desugaring for java.time on API 21+
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}

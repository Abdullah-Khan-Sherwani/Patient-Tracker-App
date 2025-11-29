plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
    alias(libs.plugins.google.services)    // ✅ resolves version from libs.versions.toml
}


android {
    namespace = "com.example.patienttracker"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }


    defaultConfig {
        applicationId = "com.example.patienttracker"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", "\"https://eqluxmkkuhypeyoitmok.supabase.co\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVxbHV4bWtrdWh5cGV5b2l0bW9rIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQ0MTQwMDksImV4cCI6MjA3OTk5MDAwOX0.nsm1-ByYuOz66vqs6qu-rcgnA-_pnI2im1_Pz-BEOGk\"")
    }



    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    // Core Jetpack & Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // 🔹 Add these for navigation & JSON support
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime:1.6.0") // or newer
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // 🔥 Firebase dependencies
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Supabase Kotlin SDK
    implementation("io.github.jan-tennert.supabase:supabase-kt:2.5.2")
    implementation("io.github.jan-tennert.supabase:storage-kt:2.5.2")
    
    // Ktor client engine (required for Supabase)
    implementation("io.ktor:ktor-client-android:2.3.11")
    implementation("io.ktor:ktor-client-core:2.3.11")
    implementation("io.ktor:ktor-utils:2.3.11")
    
    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")


    // Testing (keep as is)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "fr.kevw.kenemimusic"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.kevw.kenemimusic"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // Compose BOM — version plus récente
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Coil
//    implementation("io.coil-kt:coil-transformations:2.5.0")
    implementation("io.coil-kt:coil-compose:2.5.0")
//    implementation("io.coil-kt:coil:2.5.0")
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Compose runtime
    implementation("androidx.compose.runtime:runtime")

    // Activity Compose
    implementation("androidx.activity:activity-compose:1.9.3")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Core KTX
    implementation("androidx.core:core-ktx:1.15.0")

    // Media
    implementation("androidx.media:media:1.7.0")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.compose.foundation:foundation")

}
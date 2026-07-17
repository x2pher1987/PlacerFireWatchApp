import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

// The Live Fire Map needs a Google Maps API key. It's read from the
// gitignored local.properties (never committed) so a real key never ends
// up in source control; if it's absent (e.g. on CI, or a fresh clone),
// this falls back to an obviously-fake placeholder so the build still
// compiles — the map just won't load tiles until a real key is added.
// See the README's "Live Fire Map (Google Maps setup)" section.
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}
val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY") ?: "REPLACE_WITH_YOUR_MAPS_API_KEY"

android {
    namespace = "com.placer.firewatch"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.placer.firewatch"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-service:2.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")

    // CameraX - live preview + frame analysis
    val cameraxVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // GPS location for alert messages
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // One-tap fire reporting: Firestore for report records, Storage for attached
    // photos, anonymous Auth to identify a report's submitter, Messaging as the
    // infrastructure for a future push-based alert channel (see ROADMAP.md V2-6)
    implementation(platform("com.google.firebase:firebase-bom:34.16.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-messaging")

    // Firestore pulls in gRPC's own Guava dependency, which otherwise collides
    // with the ListenableFuture stub CameraX expects (compile error: "Cannot
    // access class 'com.google.common.util.concurrent.ListenableFuture'").
    // Forcing one consistent Android-flavored Guava across the graph fixes it.
    implementation("com.google.guava:guava:33.6.0-android")

    // Responder dashboard: live incident list + status updates.
    // Pinned to 1.3.2 (not the newer 1.4.0) because 1.4.0 requires
    // compileSdk 35+, and AGP 8.4.0 here only supports up to compileSdk 34.
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("io.coil-kt:coil:2.7.0")

    // Live Fire Map
    implementation("com.google.android.gms:play-services-maps:20.0.0")

    // Optional ML upgrade path for smoke/fire classification
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}

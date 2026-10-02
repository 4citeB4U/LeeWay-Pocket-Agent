plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android {
    namespace = "industries.leeway.pocket"
    compileSdk = 35
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    defaultConfig {
        applicationId = "industries.leeway.pocket"
        minSdk = 31
        targetSdk = 35
        versionCode = 19
        versionName = "0.3.0-unified-agent-lee-rc1"
    }
}

dependencies {
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.30.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.17.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}

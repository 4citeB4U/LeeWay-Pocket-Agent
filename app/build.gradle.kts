[Reading 29 lines from start (total: 29 lines, 0 remaining)]

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
        versionName = "0.2.17-live-voice-stream-rc2"
    }
}

dependencies {
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.30.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
}

[executed on device: localhost (0580364a-68c5-43c9-ad40-f0261d31d7b4)]
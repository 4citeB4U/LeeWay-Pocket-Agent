plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android {
    namespace = "industries.leeway.pocket"
    compileSdk = 35
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
        versionCode = 4
        versionName = "0.2.2-workstation-rc2"
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
}

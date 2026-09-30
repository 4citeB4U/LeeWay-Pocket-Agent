plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android {
    namespace = "industries.leeway.pocket"
    compileSdk = 35
    defaultConfig {
        applicationId = "industries.leeway.pocket"
        minSdk = 31
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0-workstation-rc1"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
}

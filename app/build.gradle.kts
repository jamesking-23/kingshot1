plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ejcapital.kingshot"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ejcapital.kingshot"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1-prototype"
    }

    kotlinOptions { jvmTarget = "17" }
}

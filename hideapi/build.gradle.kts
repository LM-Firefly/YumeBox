@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.library")
}

android {
    namespace = "android"
    compileSdk = 36
    
    defaultConfig {
        minSdk = 24
        
        consumerProguardFiles("consumer-rules.pro")
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // No dependencies - pure Java stubs for Android hidden APIs
}

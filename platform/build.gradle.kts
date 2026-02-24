
plugins {
    id("com.android.library")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("yumebox.base.android")
}

android {
    namespace = "com.github.yumelira.yumebox.core.android"

    buildFeatures {
        compose = true
        buildConfig = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:${gropify.dep.version.composeBom}")
    implementation(composeBom)
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.core:core-ktx:${gropify.dep.version.coreKtx}")
    implementation("com.android.tools.build:apksig:${gropify.dep.version.apksig}")
    implementation("com.jakewharton.timber:timber:${gropify.dep.version.timber}")
}



plugins {
    id("com.android.library")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.github.yumelira.yumebox.feature.update"

    buildFeatures {
        buildConfig = false
    }
}

dependencies {
    implementation(project(":data"))
    implementation(project(":locale"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${gropify.dep.version.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${gropify.dep.version.serializationJson}")
    implementation("androidx.lifecycle:lifecycle-viewmodel:${gropify.dep.version.lifecycle}")
    implementation("com.squareup.okhttp3:okhttp:${gropify.dep.version.okhttp}")
    implementation("com.jakewharton.timber:timber:${gropify.dep.version.timber}")
    implementation("androidx.core:core-ktx:${gropify.dep.version.coreKtx}")
}

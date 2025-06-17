import core.mmkvDependencyNotation

plugins {
    id("com.android.library")
    kotlin("plugin.serialization")
    id("yumebox.base.android")
}

val mmkvDependency = project.mmkvDependencyNotation()

android {
    namespace = "com.github.yumelira.yumebox.runtime.service"

    buildFeatures {
        buildConfig = false
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":common"))
    implementation(project(":locale"))
    implementation(project(":data:log"))
    implementation(project(":data:settings"))
    implementation(project(":runtime:api"))

    implementation("androidx.core:core-ktx:${gropify.dep.version.coreKtx}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${gropify.dep.version.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${gropify.dep.version.serializationJson}")
    implementation(mmkvDependency)
    implementation("com.jakewharton.timber:timber:${gropify.dep.version.timber}")
    implementation("com.squareup.okhttp3:okhttp:${gropify.dep.version.okhttp}")
}



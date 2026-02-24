import core.mmkvDependencyNotation

plugins {
    id("com.android.library")
    kotlin("plugin.serialization")
    id("yumebox.base.android")
}

val mmkvDependency = project.mmkvDependencyNotation()

android {
    namespace = "com.github.yumelira.yumebox.runtime.client"

    buildFeatures {
        buildConfig = false
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":data:settings"))
    implementation(project(":runtime:api"))

    // Temporary: local in-process gateway still directly instantiates runtime service managers.
    implementation(project(":runtime:service"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${gropify.dep.version.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${gropify.dep.version.serializationJson}")
    implementation(mmkvDependency)
    implementation("io.insert-koin:koin-core:${gropify.dep.version.koin}")
    implementation("com.jakewharton.timber:timber:${gropify.dep.version.timber}")
}




plugins {
    id("com.android.library")
    kotlin("plugin.serialization")
    id("yumebox.base.android")
}

android {
    namespace = "com.github.yumelira.yumebox.data.proxy"

    buildFeatures {
        buildConfig = false
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":data:settings"))
    implementation(project(":runtime:api"))
    implementation(project(":runtime:client"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${gropify.dep.version.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${gropify.dep.version.serializationJson}")
    implementation("io.ktor:ktor-client-core:${gropify.dep.version.ktor}")
    implementation("io.ktor:ktor-client-android:${gropify.dep.version.ktor}")
    implementation("io.ktor:ktor-client-content-negotiation:${gropify.dep.version.ktor}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${gropify.dep.version.ktor}")
    implementation("com.jakewharton.timber:timber:${gropify.dep.version.timber}")
}



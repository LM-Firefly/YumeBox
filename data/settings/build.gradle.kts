import core.mmkvDependencyNotation

plugins {
    id("com.android.library")
    kotlin("plugin.serialization")
    id("yumebox.base.android")
}

val mmkvDependency = project.mmkvDependencyNotation()

android {
    namespace = "com.github.yumelira.yumebox.data.settings"

    buildFeatures {
        buildConfig = false
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":locale"))

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${gropify.dep.version.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${gropify.dep.version.serializationJson}")

    // Keep MMKV as compileOnly here, but version must match app/runtime modules per ABI.
    compileOnly(mmkvDependency)
}



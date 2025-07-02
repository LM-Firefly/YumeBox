plugins {
    id("com.android.library")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("yumebox.base.android")
}

android {
    namespace = "com.github.yumelira.yumebox.feature.override"

    buildFeatures {
        compose = true
        buildConfig = false
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":locale"))
    implementation(project(":ui"))
    implementation(project(":data:proxy"))

    val composeBom = platform("androidx.compose:compose-bom:${gropify.dep.version.composeBom}")
    implementation(composeBom)
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:${gropify.dep.version.activityCompose}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${gropify.dep.version.lifecycle}")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${gropify.dep.version.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${gropify.dep.version.serializationJson}")
    implementation("io.insert-koin:koin-core:${gropify.dep.version.koin}")
    implementation("io.insert-koin:koin-android:${gropify.dep.version.koin}")
    implementation("io.insert-koin:koin-androidx-compose:${gropify.dep.version.koin}")
    implementation("io.github.raamcosta.compose-destinations:core:${gropify.dep.version.composeDestinations}")
    implementation("com.jakewharton.timber:timber:${gropify.dep.version.timber}")
    implementation("top.yukonga.miuix.kmp:miuix:${gropify.dep.version.miuix}")
    implementation("top.yukonga.miuix.kmp:miuix-icons:${gropify.dep.version.miuix}")
}

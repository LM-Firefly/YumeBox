
plugins {
    id("com.android.library")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
    id("yumebox.base.android")
}

android {
    namespace = "com.github.yumelira.yumebox.feature.substore"

    buildFeatures {
        compose = true
        buildConfig = false
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":platform"))
    implementation(project(":common"))
    implementation(project(":locale"))
    implementation(project(":ui"))
    implementation(project(":data:settings"))

    val composeBom = platform("androidx.compose:compose-bom:${gropify.dep.version.composeBom}")
    implementation(composeBom)
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${gropify.dep.version.coroutines}")
    implementation("io.insert-koin:koin-core:${gropify.dep.version.koin}")
    implementation("io.insert-koin:koin-android:${gropify.dep.version.koin}")
    implementation("io.insert-koin:koin-androidx-compose:${gropify.dep.version.koin}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${gropify.dep.version.lifecycle}")
    implementation("com.squareup.okhttp3:okhttp:${gropify.dep.version.okhttp}")
    implementation("org.apache.commons:commons-compress:${gropify.dep.version.commonsCompress}")
    implementation("com.caoccao.javet:javet-node-android:${gropify.dep.version.javetNodeAndroid}")
    implementation("com.jakewharton.timber:timber:${gropify.dep.version.timber}")
    implementation("top.yukonga.miuix.kmp:miuix:${gropify.dep.version.miuix}")
}



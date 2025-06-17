
plugins {
    id("com.android.library")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("yumebox.base.android")
}

android {
    namespace = "com.github.yumelira.yumebox.feature.update"

    buildFeatures {
        compose = true
        buildConfig = false
    }
}

dependencies {
    implementation(project(":locale"))

    val composeBom = platform("androidx.compose:compose-bom:${gropify.dep.version.composeBom}")
    implementation(composeBom)
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")

    implementation("top.yukonga.miuix.kmp:miuix:${gropify.dep.version.miuix}")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${gropify.dep.version.coroutines}")
    implementation("com.jakewharton.timber:timber:${gropify.dep.version.timber}")

    implementation("com.taobao.android:update-main:${gropify.dep.version.taobaoUpdate}")
    implementation("com.taobao.android:update-common:${gropify.dep.version.taobaoUpdate}")
    implementation("com.taobao.android:update-datasource:${gropify.dep.version.taobaoUpdate}")
    implementation("com.taobao.android:update-adapter:${gropify.dep.version.taobaoUpdate}")
}



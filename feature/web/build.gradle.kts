
plugins {
    id("com.android.library")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("yumebox.base.android")
}

android {
    namespace = "com.github.yumelira.yumebox.feature.web"

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
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:${gropify.dep.version.activityCompose}")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:${gropify.dep.version.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${gropify.dep.version.lifecycle}")
    implementation("top.yukonga.miuix.kmp:miuix:${gropify.dep.version.miuix}")
}



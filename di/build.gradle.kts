import core.mmkvDependencyNotation

plugins {
    id("com.android.library")
    id("yumebox.base.android")
}

val mmkvDependency = project.mmkvDependencyNotation()

android {
    namespace = "com.github.yumelira.yumebox.core.di"

    buildFeatures {
        buildConfig = false
    }
}

dependencies {
    implementation(project(":data:log"))
    implementation(project(":data:proxy"))
    implementation(project(":data:settings"))
    implementation(project(":runtime:client"))

    implementation(mmkvDependency)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${gropify.dep.version.coroutines}")
    implementation("io.insert-koin:koin-core:${gropify.dep.version.koin}")
    implementation("io.insert-koin:koin-android:${gropify.dep.version.koin}")
}

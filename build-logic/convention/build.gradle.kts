import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

val jvmVersionInt = providers.gradleProperty("project.jvm").orNull?.toIntOrNull() ?: 21
val buildLogicGroup = providers.gradleProperty("project.namespace.buildlogic").orNull
    ?: "com.github.yumelira.yumebox.buildlogic"

group = buildLogicGroup

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

val maxJvmForCompilation = minOf(jvmVersionInt, 21)
kotlin {
    jvmToolchain(maxJvmForCompilation)
    val kotlinJvmTargetInt = maxJvmForCompilation
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(kotlinJvmTargetInt.toString())
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("baseAndroid") {
            id = "yumebox.base.android"
            implementationClass = "plugins.BaseAndroidPlugin"
        }
        register("golangConfig") {
            id = "yumebox.golang.config"
            implementationClass = "plugins.GolangConfigPlugin"
        }
        register("golangTasks") {
            id = "yumebox.golang.tasks"
            implementationClass = "plugins.GolangTasksPlugin"
        }
    }
}

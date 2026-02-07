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

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(jvmVersionInt.toString())
    }
}

dependencies {
    compileOnly("com.android.tools.build:gradle:9.0.0")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.10")
    compileOnly("org.jetbrains.compose:compose-gradle-plugin:1.10.0")
    compileOnly("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.3.5")
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

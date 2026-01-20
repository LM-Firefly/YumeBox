/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) YumeYuka & YumeLira 2025.
 *
 */

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
    compileOnly("com.android.tools.build:gradle:9.0.1")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.10")
    compileOnly("org.jetbrains.compose:compose-gradle-plugin:1.10.1")
    compileOnly("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.3.6")
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

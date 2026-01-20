@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.tasks.PackageAndroidArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension

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

plugins {
    id("com.android.library") apply false
    id("com.android.application") apply false
    id("yumebox.base.android")
}

val isMergeBuild = System.getProperty("isMergeBuild") == "true"
if (isMergeBuild) {
    apply(plugin = "com.android.library")
} else {
    apply(plugin = "com.android.application")
}

project.pluginManager.withPlugin("com.android.application") {
    dependencies {
        add("implementation", "com.caoccao.javet:javet-node-android:5.0.4")
    }
}

project.pluginManager.withPlugin("com.android.library") {
    dependencies {
        add("implementation", "com.caoccao.javet:javet-node-android:5.0.4")
    }
}

val extensionJvmTarget = gropify.project.jvm.toString()
val extensionAbiList = gropify.abi.extension.list.split(",").map { it.trim() }

project.pluginManager.withPlugin("com.android.application") {
    extensions.configure<ApplicationExtension> {
        namespace = gropify.project.namespace.extension
        compileSdk = gropify.android.compileSdk
        defaultConfig {
            applicationId = gropify.project.namespace.extension
            minSdk = gropify.android.minSdk
            targetSdk = gropify.android.targetSdk
            versionCode = gropify.project.version.code
            versionName = gropify.project.version.name
        }
        tasks.withType<PackageAndroidArtifact> {
            doFirst { appMetadata.asFile.orNull?.writeText("") }
        }
        packaging {
            jniLibs {
                useLegacyPackaging = true
            }
            resources {
                excludes += listOf("META-INF/**")
            }
        }
        dependenciesInfo {
            includeInApk = false
            includeInBundle = false
        }
        buildTypes {
            debug {
                isMinifyEnabled = false
            }
            release {
                isMinifyEnabled = true
                if (!isMergeBuild) {
                    isShrinkResources = true
                }
                vcsInfo.include = false
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            }
        }
        splits {
            abi {
                isEnable = true
                reset()
                include(*extensionAbiList.toTypedArray())
                isUniversalApk = false
            }
        }
    }
}

project.pluginManager.withPlugin("com.android.library") {
    extensions.configure<LibraryExtension> {
        namespace = gropify.project.namespace.extension
        compileSdk = gropify.android.compileSdk
        defaultConfig {
            minSdk = gropify.android.minSdk
        }
        packaging {
            jniLibs {
                useLegacyPackaging = true
            }
            resources {
                excludes += setOf(
                    "/META-INF/{AL2.0,LGPL2.1}",
                    "/META-INF/*.kotlin_module",
                    "DebugProbesKt.bin",
                )
            }
        }
        splits {
            abi {
                isEnable = true
                reset()
                include(*extensionAbiList.toTypedArray())
                isUniversalApk = false
            }
        }
    }
}

@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.tasks.PackageAndroidArtifact

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
    val isMergeBuild = System.getProperty("isMergeBuild") == "true"
    if (isMergeBuild) {
        alias(libs.plugins.android.library)
    } else {
        alias(libs.plugins.android.application)
    }
    id("yumebox.base.android")
}

dependencies {
    implementation(libs.javet.node.android)
}

val extensionJvmTarget = gropify.project.jvm.toString()
val extensionAbiList = gropify.abi.extension.list.split(",").map { it.trim() }
val isMergeBuild = System.getProperty("isMergeBuild") == "true"

android {
    namespace = gropify.project.namespace.extension
    compileSdk = gropify.android.compileSdk

    defaultConfig {
        minSdk = gropify.android.minSdk
    }

    if (!isMergeBuild) {
        configure<com.android.build.api.dsl.ApplicationExtension> {
            defaultConfig {
                applicationId = gropify.project.namespace.extension
                versionCode = gropify.project.version.code
                versionName = gropify.project.version.name
                targetSdk = gropify.android.targetSdk
            }
            splits {
                abi {
                    isEnable = true
                    reset()
                    //noinspection ChromeOsAbiSupport
                    include(*extensionAbiList.toTypedArray())
                    isUniversalApk = false
                }
            }
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += listOf("META-INF/**")
        }
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
}

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
 * Copyright (c)  YumeLira 2025 - Present
 *
 */

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension

plugins {
    id("com.android.application") version "9.0.0-alpha06" apply false
    id("com.android.library") version "9.0.0-alpha06" apply false
    kotlin("plugin.serialization") version "2.2.10" apply false
    kotlin("plugin.compose") version "2.3.10" apply false
    id("org.jetbrains.compose") version "1.10.3" apply false
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
    id("com.mikepenz.aboutlibraries.plugin.android") version "14.0.0-b03" apply false
}

val androidCompileSdk = providers.gradleProperty("android.compileSdk").map(String::toInt).get()
val androidCompileSdkMinor = providers.gradleProperty("android.compileSdkMinor").map(String::toInt).orElse(0).get()
val androidMinSdk = providers.gradleProperty("android.minSdk").map(String::toInt).get()
val androidJvm = providers.gradleProperty("android.jvm")
    .orElse(providers.gradleProperty("project.jvm"))
    .orElse("17")
    .get()
val androidNdkVersion = providers.gradleProperty("android.ndkVersion").orNull.orEmpty()

subprojects {
    pluginManager.withPlugin("com.android.application") {
        extensions.configure<ApplicationExtension>("android") {
            compileSdk = androidCompileSdk
            compileSdkMinor = androidCompileSdkMinor

            if (androidNdkVersion.isNotBlank()) {
                ndkVersion = androidNdkVersion
            }

            defaultConfig {
                minSdk = androidMinSdk
            }

            compileOptions {
                sourceCompatibility = JavaVersion.toVersion(androidJvm)
                targetCompatibility = JavaVersion.toVersion(androidJvm)
            }

            sourceSets {
                getByName("main") {
                    kotlin.srcDirs("src")
                    res.srcDirs("res")
                    assets.srcDirs("assets")
                    aidl.srcDirs("aidl")
                    resources.srcDirs("resources")
                    if (project.file("AndroidManifest.xml").isFile) {
                        manifest.srcFile("AndroidManifest.xml")
                    }
                }
            }
        }
    }

    pluginManager.withPlugin("com.android.library") {
        extensions.configure<LibraryExtension>("android") {
            compileSdk = androidCompileSdk
            compileSdkExtension = androidCompileSdkMinor

            if (androidNdkVersion.isNotBlank()) {
                ndkVersion = androidNdkVersion
            }

            defaultConfig {
                minSdk = androidMinSdk
            }

            compileOptions {
                sourceCompatibility = JavaVersion.toVersion(androidJvm)
                targetCompatibility = JavaVersion.toVersion(androidJvm)
            }

            buildFeatures {
                buildConfig = false
            }

            packaging {
                jniLibs {
                    useLegacyPackaging = true
                }
            }

            sourceSets {
                getByName("main") {
                    kotlin.srcDirs("src")
                    res.srcDirs("res")
                    assets.srcDirs("assets")
                    aidl.srcDirs("aidl")
                    resources.srcDirs("resources")
                    if (project.file("AndroidManifest.xml").isFile) {
                        manifest.srcFile("AndroidManifest.xml")
                    }
                }
            }
        }
    }
}

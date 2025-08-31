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

plugins {
    id("com.android.library")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("dev.oom-wg.purejoy.fyl.fytxt")
}

fytxt {
    langSrcs = mapOf(
        "lang" to layout.projectDirectory.dir("lang"),
    )
    packageName = "dev.oom_wg.purejoy.mlang"
    objectName = "MLang"
    defaultLang = "ZH"
    composeGen = true
    internalClass = false
}

android {
    namespace = "com.github.yumelira.yumebox.core.locale"
    compileSdk = gropify.android.compileSdk

    val ndkVersionValue = gropify.android.ndkVersion
    if (ndkVersionValue.isNotBlank()) {
        ndkVersion = ndkVersionValue
    }

    defaultConfig {
        minSdk = gropify.android.minSdk
    }

    compileOptions {
        val javaVer = gropify.android.jvm ?: gropify.project.jvm ?: "17"
        sourceCompatibility = JavaVersion.toVersion(javaVer)
        targetCompatibility = JavaVersion.toVersion(javaVer)
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/*.kotlin_module",
                "DebugProbesKt.bin",
            )
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src", "build/generated/fytxt/kotlin/commonMain/kotlin")
            res.srcDirs("res")
            assets.srcDirs("assets")
            aidl.srcDirs("aidl")
            resources.srcDirs("resources")
            if (project.file("AndroidManifest.xml").isFile) {
                manifest.srcFile("AndroidManifest.xml")
            }
        }
        getByName("test") {
            java.setSrcDirs(emptyList<String>())
            resources.setSrcDirs(emptyList<String>())
            assets.setSrcDirs(emptyList<String>())
        }
        getByName("androidTest") {
            java.setSrcDirs(emptyList<String>())
            res.setSrcDirs(emptyList<String>())
            assets.setSrcDirs(emptyList<String>())
            aidl.setSrcDirs(emptyList<String>())
            resources.setSrcDirs(emptyList<String>())
        }
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:${gropify.dep.version.composeBom}")
    implementation(composeBom)
    implementation("androidx.compose.runtime:runtime")
}
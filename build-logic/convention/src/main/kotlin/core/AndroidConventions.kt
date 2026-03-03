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

package core

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project

internal data class AndroidConventionValues(
    val compileSdk: Int,
    val minSdk: Int,
    val jvmVersion: JavaVersion,
    val ndkVersion: String,
)

internal fun Project.readAndroidConventionValues(): AndroidConventionValues {
    val provider = ConfigProvider(this)
    val jvmVersionString = provider.getString("android.jvm", provider.getString("project.jvm", "17"))
    return AndroidConventionValues(
        compileSdk = provider.getInt("android.compileSdk", 34),
        minSdk = provider.getInt("android.minSdk", 24),
        jvmVersion = JavaVersion.toVersion(jvmVersionString),
        ndkVersion = provider.getString("android.ndkVersion", ""),
    )
}

internal fun ApplicationExtension.applyBaseAndroidConvention(values: AndroidConventionValues) {
    compileSdk = values.compileSdk
    defaultConfig {
        minSdk = values.minSdk
    }
    compileOptions {
        sourceCompatibility = values.jvmVersion
        targetCompatibility = values.jvmVersion
    }
    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/*.kotlin_module",
                "DebugProbesKt.bin",
            )
        }
        jniLibs { useLegacyPackaging = true }
    }
}

internal fun LibraryExtension.applyBaseAndroidConvention(values: AndroidConventionValues) {
    compileSdk = values.compileSdk
    if (values.ndkVersion.isNotBlank()) {
        ndkVersion = values.ndkVersion
    }
    defaultConfig {
        minSdk = values.minSdk
    }
    compileOptions {
        sourceCompatibility = values.jvmVersion
        targetCompatibility = values.jvmVersion
    }
    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/*.kotlin_module",
                "DebugProbesKt.bin",
            )
        }
        jniLibs { useLegacyPackaging = true }
    }
}

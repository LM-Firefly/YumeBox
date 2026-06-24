/*
 * This file is part of FlyCat.
 *
 * FlyCat is free software: you can redistribute it and/or modify
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
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.tasks.MergeSourceSetFolders
import java.time.format.DateTimeFormatter
import java.time.ZonedDateTime
import java.time.ZoneId
import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

val appAbiList =
    providers.gradleProperty("abi.app.list").get().split(',').map { it.trim() }.filter { it.isNotEmpty() }

val projectApplicationId = providers.gradleProperty("project.applicationId")
    .orElse(providers.gradleProperty("project.namespace.base"))
    .get()
val updateRepository = providers.gradleProperty("update.repository").orNull
    ?.trim()
    ?.ifEmpty { null }
    ?: "LM-Firefly/FlyCat"
val updateSource = providers.gradleProperty("update.source").orNull
    ?.trim()
    ?.ifEmpty { null }
    ?: "smart"
val updateUiBuildStamp = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
val updateUiCommitShort = runCatching {
    providers.exec {
        commandLine("git", "rev-parse", "--short=6", "HEAD")
        workingDir = rootDir
    }.standardOutput.asText.get().trim().ifBlank { "000000" }
}.getOrDefault("000000")
val updateUiBuildId = providers.gradleProperty("update.uiBuildId").orNull
    ?.trim()
    ?.ifEmpty { null }
    ?: "${updateUiBuildStamp}-${updateUiCommitShort}"
val updateMirrorTemplates = providers.gradleProperty("update.mirrorTemplates").orNull
    ?.trim()
    ?.ifEmpty { null }
    ?: ""

fun String.asBuildConfigString(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = providers.gradleProperty("project.namespace.base").get()

    defaultConfig {
        applicationId = "$projectApplicationId.lite"
        targetSdk = providers.gradleProperty("android.targetSdk").get().toInt()
        versionCode = providers.gradleProperty("project.version.code").get().toInt()
        versionName = providers.gradleProperty("project.version.name").get()
        manifestPlaceholders["appName"] = "${providers.gradleProperty("project.name").get()} Lite"
        buildConfigField("String", "UPDATE_REPOSITORY", updateRepository.asBuildConfigString())
        buildConfigField("String", "UPDATE_SOURCE", updateSource.asBuildConfigString())
        buildConfigField("String", "UI_BUILD_ID", updateUiBuildId.asBuildConfigString())
        buildConfigField("String", "UPDATE_MIRROR_TEMPLATES", updateMirrorTemplates.asBuildConfigString())
    }

    compileOptions {
        val javaVer = providers.gradleProperty("android.jvm").get()
        sourceCompatibility = JavaVersion.toVersion(javaVer)
        targetCompatibility = JavaVersion.toVersion(javaVer)
        isCoreLibraryDesugaringEnabled = true
    }

    sourceSets {
        getByName("main") {
            kotlin.directories.apply {
                clear()
                add("src")
            }
            res.directories.apply {
                clear()
                addAll(
                    listOf(
                        "res",
                        "../app/res",
                    )
                )
            }
            assets.directories.apply {
                clear()
                addAll(
                    listOf(
                        "assets",
                        "../app/assets",
                    )
                )
            }
            aidl.directories.apply {
                clear()
                add("aidl")
            }
            resources.directories.apply {
                clear()
                addAll(
                    listOf(
                        "resources",
                        "../app/resources",
                    )
                )
            }
            jniLibs.directories.apply {
                clear()
                add("../jniLibs")
            }
            if (project.file("AndroidManifest.xml").isFile) {
                manifest.srcFile("AndroidManifest.xml")
            }
        }
    }

    androidResources {
        generateLocaleConfig = false
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            vcsInfo.include = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    splits {
        abi {
            isEnable = gradle.startParameter.taskNames.none { it.contains("bundle", ignoreCase = true) }
            reset()
            include(*appAbiList.toTypedArray())
            isUniversalApk = true
        }
    }

    packaging {
        jniLibs {
            excludes += listOf(
                "lib/**/libjavet*.so",
            )
            useLegacyPackaging = true
        }
    }

    signingConfigs {
        val keystore = rootProject.file("signing.properties")
        if (keystore.exists()) {
            create("release") {
                val prop = Properties().apply { keystore.inputStream().use(::load) }
                storeFile = rootProject.file("release.keystore")
                storePassword = prop.getProperty("keystore.password")!!
                keyAlias = prop.getProperty("key.alias")!!
                keyPassword = prop.getProperty("key.password")!!
            }
        }
    }

    if (signingConfigs.findByName("release") != null) {
        buildTypes.named("release").configure {
            signingConfig = signingConfigs.getByName("release")
        }
        buildTypes.named("debug").configure {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    //noinspection WrongGradleMethod
    androidComponents {
        onVariants { variant ->
            variant.outputs.forEach { output ->
                val abiName = output.filters.find {
                    it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI
                }?.identifier ?: "universal"
                val buildTypeName = variant.buildType ?: "release"
                output.versionName.set(providers.gradleProperty("project.version.name").get())
                (output as com.android.build.api.variant.impl.VariantOutputImpl).outputFileName.set(
                    "${providers.gradleProperty("project.name").get()}-lite-${abiName}-${buildTypeName}-${updateUiBuildId}.apk"
                )
            }
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(project(":core"))
    implementation(project(":platform"))
    implementation(project(":locale"))
    implementation(project(":ui"))
    implementation(project(":data"))
    implementation(project(":runtime:api"))
    implementation(project(":runtime:client"))
    runtimeOnly(project(":runtime:service"))
    implementation(project(":feature:proxy"))
    implementation(project(":feature:update"))

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation(libs.androidx.activity.compose)
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation(libs.miuix.ui)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.blur.android)
    implementation(libs.haze)
    implementation(libs.haze.blur)

    val mmkv64 = libs.versions.mmkv64.get()
    val mmkv32 = libs.versions.mmkv32.get()
    val injectedAbi = findProperty("android.injected.build.abi") as? String
    val mmkvVersion = if (injectedAbi in listOf("arm64-v8a", "x86_64")) mmkv64 else mmkv32
    //noinspection NewerVersionAvailable
    implementation("com.tencent:mmkv:$mmkvVersion")
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)

    implementation(libs.androidx.navigationevent.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    implementation(libs.timber)
    implementation(libs.xz)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}

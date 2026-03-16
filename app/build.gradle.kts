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

@file:Suppress("UnstableApiUsage")

import groovy.json.JsonSlurper
import java.util.*

plugins {
    id("com.android.application")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("com.google.devtools.ksp")
    id("com.mikepenz.aboutlibraries.plugin.android")
    id("io.sentry.android.gradle") version "6.2.0"
}


val appAbiList = (gropify.abi.app.list ?: "armeabi-v7a,arm64-v8a,x86,x86_64")
    .split(',').map { it.trim() }.filter { it.isNotEmpty() }

data class EmasConfig(val appKey: String, val appSecret: String, val channelId: String)

val emasConfig = runCatching {
    val file = layout.projectDirectory.file("aliyun-emas-services.json").asFile
    if (file.exists()) {
        val json = JsonSlurper().parse(file) as? Map<*, *>
        val config = json?.get("config") as? Map<*, *> ?: emptyMap<Any, Any>()
        EmasConfig(
            appKey = (config["emas.appKey"] ?: "").toString(),
            appSecret = (config["emas.appSecret"] ?: "").toString(),
            channelId = (config["emas.channelId"] ?: "official").toString()
        )
    } else {
        EmasConfig("", "", "official")
    }
}.getOrDefault(EmasConfig("", "", "official"))

val geoFilesAssetsDir = rootProject.layout.buildDirectory.dir("generated/assets/geo")


android {
    namespace = gropify.project.namespace.base
    compileSdk = gropify.android.compileSdk

    defaultConfig {
        minSdk = gropify.android.minSdk
        applicationId = gropify.project.namespace.base
        targetSdk = gropify.android.targetSdk
        versionCode = gropify.project.version.code
        versionName = gropify.project.version.name
        manifestPlaceholders["appName"] = gropify.project.name

        buildConfigField("String", "CLARITY_PROJECT_ID", "\"${gropify.clarity.projectId}\"")
        buildConfigField("String", "EMAS_APP_KEY", "\"${emasConfig.appKey}\"")
        buildConfigField("String", "EMAS_APP_SECRET", "\"${emasConfig.appSecret}\"")
        buildConfigField("String", "EMAS_CHANNEL_ID", "\"${emasConfig.channelId}\"")
    }

    compileOptions {
        val javaVer = gropify.android.jvm ?: gropify.project.jvm ?: "17"
        sourceCompatibility = JavaVersion.toVersion(javaVer)
        targetCompatibility = JavaVersion.toVersion(javaVer)
        isCoreLibraryDesugaringEnabled = true
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src")
            res.srcDirs("res")
            assets.srcDirs("assets")
            aidl.srcDirs("aidl")
            resources.srcDirs("resources")
            jniLibs.srcDirs("../jniLibs")
            if (project.file("AndroidManifest.xml").isFile) {
                manifest.srcFile("AndroidManifest.xml")
            }
            assets.srcDir(geoFilesAssetsDir)
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
            excludes += listOf("lib/**/libjavet*.so")
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
    }

    androidComponents {
        onVariants { variant ->
            variant.outputs.forEach { output ->
                val abiName = output.filters.find {
                    it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI
                }?.identifier ?: "universal"
                val buildTypeName = variant.buildType ?: "release"
                output.versionName.set(gropify.project.version.name)
                (output as com.android.build.api.variant.impl.VariantOutputImpl).outputFileName.set(
                    "${gropify.project.name}-${abiName}-${buildTypeName}.apk"
                )
            }
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:${gropify.dep.version.desugarJdkLibs}")

    implementation(project(":core"))
    implementation(project(":platform"))
    implementation(project(":locale"))
    implementation(project(":ui"))
    implementation(project(":data:log"))
    implementation(project(":data:settings"))
    implementation(project(":data:proxy"))
    implementation(project(":runtime:api"))
    implementation(project(":runtime:client"))
    implementation(project(":runtime:service"))
    implementation(project(":feature:substore"))
    implementation(project(":feature:proxy"))
    implementation(project(":feature:override"))
    implementation(project(":feature:editor"))
    implementation(project(":feature:meta"))

    val composeBom = platform("androidx.compose:compose-bom:${gropify.dep.version.composeBom}")
    implementation(composeBom)
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:${gropify.dep.version.activityCompose}")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("top.yukonga.miuix.kmp:miuix:${gropify.dep.version.miuix}")
    implementation("top.yukonga.miuix.kmp:miuix-icons:${gropify.dep.version.miuix}")
    implementation("dev.chrisbanes.haze:haze:${gropify.dep.version.haze}")
    implementation("io.github.fletchmckee.liquid:liquid:${gropify.dep.version.liquid}")
    implementation("androidx.navigationevent:navigationevent-compose:${gropify.dep.version.navigationevent}")

    val mmkv64 = gropify.dep.version.mmkv64
    val mmkv32 = gropify.dep.version.mmkv32
    val injectedAbi = findProperty("android.injected.build.abi") as? String
    val mmkvVersion = if (injectedAbi in listOf("arm64-v8a", "x86_64")) mmkv64 else mmkv32
    implementation("com.tencent:mmkv:$mmkvVersion")

    implementation("io.insert-koin:koin-core:${gropify.dep.version.koin}")
    implementation("io.insert-koin:koin-android:${gropify.dep.version.koin}")
    implementation("io.insert-koin:koin-androidx-compose:${gropify.dep.version.koin}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${gropify.dep.version.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${gropify.dep.version.serializationJson}")

    implementation("io.github.raamcosta.compose-destinations:core:${gropify.dep.version.composeDestinations}")
    ksp("io.github.raamcosta.compose-destinations:ksp:${gropify.dep.version.composeDestinations}")

    implementation("com.jakewharton.timber:timber:${gropify.dep.version.timber}")
    implementation("org.tukaani:xz:1.12")

    implementation("com.google.mlkit:barcode-scanning:${gropify.dep.version.mlkitBarcodeScanning}")

    implementation("androidx.camera:camera-camera2:${gropify.dep.version.camera}")
    implementation("androidx.camera:camera-lifecycle:${gropify.dep.version.camera}")
    implementation("androidx.camera:camera-view:${gropify.dep.version.camera}")
    implementation("androidx.camera:camera-core:${gropify.dep.version.camera}")
    implementation("androidx.camera:camera-video:${gropify.dep.version.camera}")

    implementation("io.coil-kt.coil3:coil-compose:${gropify.dep.version.coil3}")
    implementation("io.coil-kt.coil3:coil-network-okhttp:${gropify.dep.version.coil3}")
    implementation("io.coil-kt.coil3:coil-svg:${gropify.dep.version.coil3}")
    implementation("io.github.panpf.sketch4:sketch-compose:${gropify.dep.version.sketch4}")
    implementation("io.github.panpf.sketch4:sketch-http:${gropify.dep.version.sketch4}")
    implementation("io.github.panpf.sketch4:sketch-animated-webp:${gropify.dep.version.sketch4}")

    implementation("sh.calvin.reorderable:reorderable:${gropify.dep.version.reorderable}")
    implementation("com.mikepenz:aboutlibraries-core:${gropify.dep.version.aboutLibraries}")
    implementation("com.mikepenz:aboutlibraries-compose:${gropify.dep.version.aboutLibraries}")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:${gropify.dep.version.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:${gropify.dep.version.lifecycle}")

    implementation("com.taobao.android:update-main:${gropify.dep.version.taobaoUpdate}")
    implementation("com.taobao.android:update-common:${gropify.dep.version.taobaoUpdate}")
    implementation("com.taobao.android:update-datasource:${gropify.dep.version.taobaoUpdate}")
    implementation("com.taobao.android:update-adapter:${gropify.dep.version.taobaoUpdate}")

    implementation("com.microsoft.clarity:clarity-compose:3.8.1")
}

ksp {
    arg("compose-destinations.defaultTransitions", "none")
}

sentry {
    org.set("12d34a06e78c")
    projectName.set("android")
    includeSourceContext.set(true)
}

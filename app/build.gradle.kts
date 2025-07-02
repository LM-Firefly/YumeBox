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
 * Copyright (c)  YumeLira 2026.
 *
 */

@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.tasks.MergeSourceSetFolders
import core.mmkvDependencyNotation
import groovy.json.JsonSlurper
import tasks.DownloadGeoFilesTask

plugins {
    id("com.android.application")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("com.google.devtools.ksp")
    id("com.mikepenz.aboutlibraries.plugin.android")
    id("yumebox.base.android")
    id("yumebox.build.helpers")

    id("io.sentry.android.gradle") version "6.1.0"
}

private data class EmasConfigValues(
    val appKey: String,
    val appSecret: String,
    val channelId: String,
)

private fun Project.readEmasConfigMap(): Map<*, *> {
    val emasConfigFile = layout.projectDirectory.file("aliyun-emas-services.json").asFile
    return runCatching {
        if (!emasConfigFile.exists()) emptyMap<String, Any?>()
        else {
            val root = JsonSlurper().parse(emasConfigFile) as? Map<*, *> ?: emptyMap<Any, Any>()
            root["config"] as? Map<*, *> ?: emptyMap<Any, Any>()
        }
    }.getOrDefault(emptyMap<String, Any?>())
}

private fun Project.emasConfigValue(
    emasConfigMap: Map<*, *>,
    key: String,
    fallbackProperty: String,
    defaultValue: String = "",
): String {
    val fromJson = emasConfigMap[key]?.toString().orEmpty()
    if (fromJson.isNotBlank()) return fromJson
    return (findProperty(fallbackProperty) as? String)?.takeIf { it.isNotBlank() } ?: defaultValue
}

private fun Project.readEmasConfigValues(): EmasConfigValues {
    val configMap = readEmasConfigMap()
    return EmasConfigValues(
        appKey = emasConfigValue(configMap, "emas.appKey", "emas.appKey"),
        appSecret = emasConfigValue(configMap, "emas.appSecret", "emas.appSecret"),
        channelId = emasConfigValue(configMap, "emas.channelId", "emas.channelId", "official"),
    )
}

private fun DependencyHandlerScope.implementationProjects(vararg modules: String) {
    modules.forEach { implementation(project(it)) }
}

private fun DependencyHandlerScope.addAppProjectDependencies() {
    implementationProjects(
        ":core",
        ":platform",
        ":locale",
        ":ui",
        ":data:log",
        ":data:settings",
        ":data:proxy",
        ":runtime:api",
        ":runtime:client",
        ":runtime:service",
        ":feature:substore",
        ":feature:proxy",
        ":feature:override",
    )
}

private fun TaskContainer.registerGeoFilesDownloadTask(
    geoFilesDownloadDir: org.gradle.api.file.Directory,
    assets: Map<String, String>,
): TaskProvider<DownloadGeoFilesTask> = register<DownloadGeoFilesTask>("downloadGeoFiles") {
    description = "Download GeoIP and GeoSite databases from MetaCubeX"
    group = "build setup"
    assetUrls.putAll(assets)
    outputDirectory.set(geoFilesDownloadDir)
}

val mmkvDependency = project.mmkvDependencyNotation()

val appNamespace = gropify.project.namespace.base
val appName = gropify.project.name
val androidJvmTarget = gropify.android.jvm.toString()
val appAbiList = gropify.abi.app.list.split(",").map { it.trim() }
private val emasConfig = project.readEmasConfigValues()
val geoFilesAssetsDir = layout.buildDirectory.dir("generated/assets/geo/main")
val geoFilesDownloadDir = geoFilesAssetsDir.get()
val geoAssets = mapOf(
    "geoip.metadb.xz" to gropify.asset.geoip.url,
    "geosite.dat.xz" to gropify.asset.geosite.url,
    "ASN.mmdb.xz" to gropify.asset.asn.url,
)
val downloadGeoFilesTask = tasks.registerGeoFilesDownloadTask(geoFilesDownloadDir, geoAssets)

android {
    namespace = appNamespace

    defaultConfig {
        applicationId = appNamespace
        targetSdk = gropify.android.targetSdk
        versionCode = gropify.project.version.code
        versionName = gropify.project.version.name
        manifestPlaceholders["appName"] = appName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Clarity Analytics Configuration
        buildConfigField("String", "CLARITY_PROJECT_ID", "\"${project.findProperty("clarity.projectId") ?: ""}\"")
        buildConfigField("String", "EMAS_APP_KEY", "\"${emasConfig.appKey}\"")
        buildConfigField("String", "EMAS_APP_SECRET", "\"${emasConfig.appSecret}\"")
        buildConfigField("String", "EMAS_CHANNEL_ID", "\"${emasConfig.channelId}\"")

    }

    sourceSets {
        named("main") {
            // Explicitly package local JNI libraries from app/src/main/jniLibs
            jniLibs.directories.add("src/main/jniLibs")
            // Build-generated geo assets merged during packaging/lint
            assets.directories.add(geoFilesDownloadDir.asFile.absolutePath)
            // Let IDE index compose-destinations generated code without requiring a build refresh cycle
            // java.srcDir(layout.buildDirectory.dir("generated/ksp/debug/kotlin"))
            // java.srcDir(layout.buildDirectory.dir("generated/ksp/release/kotlin"))
        }

    }

    compileOptions { isCoreLibraryDesugaringEnabled = true }

    // noinspection WrongGradleMethod
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(androidJvmTarget))
        }
    }

    androidResources {
        // Don't generate automatic locale config, we'll specify locales manually
        generateLocaleConfig = false
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = false
        dataBinding = false
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
            isJniDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            isJniDebuggable = false
            vcsInfo.include = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    splits {
        abi {
            // noinspection WrongGradleMethod
            isEnable = gradle.startParameter.taskNames.none { it.contains("bundle", ignoreCase = true) }
            reset()
            // noinspection ChromeOsAbiSupport
            include(*appAbiList.toTypedArray())
            isUniversalApk = true
        }
    }

    packaging {
        jniLibs {
            excludes += listOf("lib/**/libjavet*.so")
            useLegacyPackaging = true
        }
        resources {
            excludes += listOf(
                "Sub-Store/**",
                "**/*.kotlin_builtins",
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json",
                "META-INF/**",
                "index.*.bin",
            )
        }
    }

    // Use new androidComponents API instead of deprecated applicationVariants
    // noinspection WrongGradleMethod
    androidComponents {
        onVariants { variant ->
            val variantNameCap = variant.name.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }

            variant.outputs.forEach { output ->
                val abiName = output.filters.find {
                    it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI
                }?.identifier ?: "universal"
                val buildTypeName = variant.buildType ?: "release"
                // Set correct versionName
                output.versionName.set(gropify.project.version.name)
                // Set APK output file name
                (output as com.android.build.api.variant.impl.VariantOutputImpl).outputFileName.set(
                    "${appName}-${abiName}-${buildTypeName}.apk"
                )
            }
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:${gropify.dep.version.desugarJdkLibs}")

    // Internal modules
    addAppProjectDependencies()

    // Compose dependencies (using Jetpack Compose BOM for version management)
    val composeBom = platform("androidx.compose:compose-bom:${gropify.dep.version.composeBom}")
    implementation(composeBom)
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:${gropify.dep.version.activityCompose}")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Additional Compose libraries
    implementation("top.yukonga.miuix.kmp:miuix:${gropify.dep.version.miuix}")
    implementation("top.yukonga.miuix.kmp:miuix-icons:${gropify.dep.version.miuix}")
    implementation("dev.chrisbanes.haze:haze:${gropify.dep.version.haze}")

    // Storage
    implementation(mmkvDependency)

    // Dependency Injection
    implementation("io.insert-koin:koin-core:${gropify.dep.version.koin}")
    implementation("io.insert-koin:koin-android:${gropify.dep.version.koin}")
    implementation("io.insert-koin:koin-androidx-compose:${gropify.dep.version.koin}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${gropify.dep.version.coroutines}")

    // Navigation
    implementation("io.github.raamcosta.compose-destinations:core:${gropify.dep.version.composeDestinations}")
    ksp("io.github.raamcosta.compose-destinations:ksp:${gropify.dep.version.composeDestinations}")

    // Utilities
    implementation("com.jakewharton.timber:timber:${gropify.dep.version.timber}")
    implementation("org.tukaani:xz:1.11")

    // ML Kit
    implementation("com.google.mlkit:barcode-scanning:${gropify.dep.version.mlkitBarcodeScanning}")

    // Camera
    implementation("androidx.camera:camera-camera2:${gropify.dep.version.camera}")
    implementation("androidx.camera:camera-lifecycle:${gropify.dep.version.camera}")
    implementation("androidx.camera:camera-view:${gropify.dep.version.camera}")
    implementation("androidx.camera:camera-core:${gropify.dep.version.camera}")
    implementation("androidx.camera:camera-video:${gropify.dep.version.camera}")

    // Image Loading
    implementation("io.coil-kt.coil3:coil-compose:${gropify.dep.version.coil3}")
    implementation("io.coil-kt.coil3:coil-network-okhttp:${gropify.dep.version.coil3}")
    implementation("io.coil-kt.coil3:coil-svg:${gropify.dep.version.coil3}")
    implementation("io.github.panpf.sketch4:sketch-compose:${gropify.dep.version.sketch4}")
    implementation("io.github.panpf.sketch4:sketch-http:${gropify.dep.version.sketch4}")
    implementation("io.github.panpf.sketch4:sketch-animated-webp:${gropify.dep.version.sketch4}")

    // UI Components
    implementation("sh.calvin.reorderable:reorderable:${gropify.dep.version.reorderable}")
    implementation("com.mikepenz:aboutlibraries-core:${gropify.dep.version.aboutLibraries}")
    implementation("com.mikepenz:aboutlibraries-compose:${gropify.dep.version.aboutLibraries}")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:${gropify.dep.version.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:${gropify.dep.version.lifecycle}")

    // App Update (EMAS)
    implementation("com.taobao.android:update-main:${gropify.dep.version.taobaoUpdate}")
    implementation("com.taobao.android:update-common:${gropify.dep.version.taobaoUpdate}")
    implementation("com.taobao.android:update-datasource:${gropify.dep.version.taobaoUpdate}")
    implementation("com.taobao.android:update-adapter:${gropify.dep.version.taobaoUpdate}")

    implementation("com.microsoft.clarity:clarity-compose:3.8.1")

}

ksp {
    arg("compose-destinations.defaultTransitions", "none")
}

tasks.withType<MergeSourceSetFolders>().configureEach {
    dependsOn(downloadGeoFilesTask)
}

// Lint Vital tasks may read src/main/assets directly and bypass MergeSourceSetFolders,
// so Gradle 9 validation requires an explicit dependency on downloadGeoFiles.
tasks.configureEach {
    if (
        name.startsWith("lintVitalAnalyze") ||
        (name.startsWith("generate") && name.contains("LintVitalReportModel"))
    ) {
        dependsOn(downloadGeoFilesTask)
    }
}

tasks.register<Delete>("cleanGeoFiles") {
    description = "Clean downloaded GeoIP and GeoSite databases"
    group = "build setup"
    delete(geoFilesAssetsDir)
}

aboutLibraries {
    export {
        outputFile = file("src/main/resources/aboutlibraries.json")
    }
}


sentry {
    org.set("12d34a06e78c")
    projectName.set("android")

    // this will upload your source code to Sentry to show it as part of the stack traces
    // disable if you don't want to expose your sources
    includeSourceContext.set(true)
}

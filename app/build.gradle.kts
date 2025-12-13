@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.tasks.MergeSourceSetFolders
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties

abstract class DownloadGeoFilesTask : DefaultTask() {
    @get:Input
    abstract val assetUrls: MapProperty<String, String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun download() {
        val destinationDir = outputDirectory.get().asFile
        destinationDir.mkdirs()

        assetUrls.get().forEach { (fileName, url) ->
            val outputFile = destinationDir.resolve(fileName)
            runCatching {
                val uri = URI(url)
                uri.toURL().openStream().use { input ->
                    Files.copy(input, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
                logger.lifecycle("$fileName downloaded to ${outputFile.absolutePath}")
            }.onFailure { error ->
                logger.warn("Failed to download $fileName from $url", error)
            }
        }
    }
}



plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.purejoy.mlang)
    // Firebase and Google Services plugins removed
}


MLang {
    name = null
    configDir = "../lang"
    baseLang = "zh"
    base = true
    compose = true
}

val targetAbi = project.findProperty("android.injected.build.abi") as String?
val mmkvDependency = when (targetAbi) {
    "arm64-v8a", "x86_64" -> libs.mmkv
    else -> libs.mmkv.legacy
}

val appNamespace = gropify.project.namespace.base
val appName = gropify.project.name
val jvmVersionNumber = gropify.project.jvm
val jvmVersion = jvmVersionNumber.toString()
val javaVersion = JavaVersion.toVersion(jvmVersionNumber)
val appAbiList = gropify.abi.app.list.split(",").map { it.trim() }
val localeList = gropify.locale.app.list.split(",").map { it.trim() }

kotlin {
    androidTarget()
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmVersionNumber))
    }
    sourceSets {
        androidMain.dependencies {
            if (System.getProperty("isMergeBuild") == "true") {
                implementation(project(":extension"))
            }
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.miuix)
            implementation(libs.haze.materials)
            implementation(mmkvDependency)
            implementation(libs.bundles.koin)
            implementation(libs.compose.destinations.core)
            implementation(libs.okhttp)
            implementation(libs.timber)
            implementation(libs.javet.node.android)
            implementation(libs.pangutext.android)
            implementation(libs.commons.compress)
            // Firebase dependencies removed: firebase-bom, firebase-crashlytics-ndk, firebase-analytics
            implementation(libs.mlkit.barcode.scanning)
            implementation(libs.bundles.camerax)
            implementation(libs.bundles.ktor)
            implementation(libs.bundles.aboutlibraries)
        }

        commonMain.dependencies {
            implementation(project(":core"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(jvmVersion))
}

android {
    namespace = appNamespace
    compileSdk = gropify.android.compileSdk

    defaultConfig {
        minSdk = gropify.android.minSdk
        targetSdk = gropify.android.targetSdk
        versionCode = gropify.project.version.code
        versionName = gropify.project.version.name
        manifestPlaceholders["appName"] = appName
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    androidResources {
        localeFilters += localeList
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = false
        dataBinding = false
    }

    signingConfigs {
        val keystore = rootProject.file("signing.properties")
        if (keystore.exists()) {
            create("release") {
                val prop = Properties().apply {
                    keystore.inputStream().use(this::load)
                }
                storeFile = rootProject.file(prop.getProperty("keystore.path") ?: "release.keystore")
                storePassword = prop.getProperty("keystore.password")!!
                keyAlias = prop.getProperty("key.alias")!!
                keyPassword = prop.getProperty("key.password")!!
            }
        }
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
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    splits {
        abi {
            //noinspection WrongGradleMethod
            isEnable = gradle.startParameter.taskNames.none { it.contains("bundle", ignoreCase = true) }
            reset()
            //noinspection ChromeOsAbiSupport
            include(*appAbiList.toTypedArray())
            isUniversalApk = false
        }
    }

    packaging {
        jniLibs {
            if (System.getProperty("isMergeBuild") != "true") {
                excludes += listOf("lib/**/libjavet*.so")
            }
            useLegacyPackaging = true
        }
        resources {
            excludes += listOf(
                "SubStore/**",
                "**/*.kotlin_builtins",
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json",
                "META-INF/**",
                "index.*.bin",
            )
        }
    }

    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val abiName = filters.find { it.filterType == "ABI" }?.identifier ?: "universal"
            val buildTypeName = buildType.name
            val isMergeBuild = System.getProperty("isMergeBuild") == "true"
            val fileName = if (isMergeBuild) {
                "${appName}_Extension-${abiName}-${buildTypeName}.apk"
            } else {
                "${appName}-${abiName}-${buildTypeName}.apk"
            }
            output.outputFileName = fileName
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    debugImplementation(compose.uiTooling)
    add("kspAndroid", libs.compose.destinations.ksp)
}

ksp {
    arg("compose-destinations.defaultTransitions", "none")
}

val geoFilesDownloadDir = layout.projectDirectory.dir("src/androidMain/assets")

val downloadGeoFilesTask = tasks.register<DownloadGeoFilesTask>("downloadGeoFiles") {
    description = "Download GeoIP and GeoSite databases from MetaCubeX"
    group = "build setup"

    val assets = mapOf(
        "geoip.metadb" to gropify.asset.geoip.url,
        "geosite.dat" to gropify.asset.geosite.url,
        "ASN.mmdb" to gropify.asset.asn.url,
    )
    assetUrls.putAll(assets)
    outputDirectory.set(geoFilesDownloadDir)
}

tasks.configureEach {
    when {
        name.startsWith("assemble") || name.startsWith("lintVitalAnalyze") || (name.startsWith("generate") && name.contains(
            "LintVitalReportModel"
        )) -> {
            dependsOn(downloadGeoFilesTask)
        }
    }
}

tasks.withType<MergeSourceSetFolders>().configureEach {
    dependsOn(downloadGeoFilesTask)
}

tasks.register<Delete>("cleanGeoFiles") {
    description = "Clean downloaded GeoIP and GeoSite databases"
    group = "build setup"
    delete(geoFilesDownloadDir)
}

aboutLibraries {
    export {
        outputFile = file("src/androidMain/resources/aboutlibraries.json")
    }
}


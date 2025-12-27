@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.tasks.PackageAndroidArtifact
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec

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

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(21)
        targetCompatibility = JavaVersion.toVersion(21)
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

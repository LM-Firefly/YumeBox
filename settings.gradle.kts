@file:Suppress("UnstableApiUsage")

rootProject.name = "YumeBox"

includeBuild("libs/PureJoy-MultiLang") {
    dependencySubstitution {
        substitute(module("dev.oom-wg.purejoy.mlang:base")).using(project(":base"))
        substitute(module("dev.oom-wg.purejoy.mlang:compose")).using(project(":compose"))
        substitute(module("dev.oom-wg.PureJoy-MultiLang:base")).using(project(":base"))
        substitute(module("dev.oom-wg.PureJoy-MultiLang:compose")).using(project(":compose"))
        substitute(module("dev.oom-wg:PureJoy-MultiLang:base")).using(project(":base"))
        substitute(module("dev.oom-wg:PureJoy-MultiLang:compose")).using(project(":compose"))
    }
}
includeBuild("libs/FVV/kotlin")

val isMergeBuild = gradle.startParameter.taskNames.any { it.contains("assembleReleaseWithExtension", ignoreCase = true) }
if (isMergeBuild) {
    System.setProperty("isMergeBuild", "true")
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

pluginManagement {
    includeBuild("build-logic")
    includeBuild("libs/PureJoy-MultiLang")
    includeBuild("libs/FVV/kotlin")
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
        maven("https://jitpack.io")
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "dev.oom-wg.purejoy.mlang") {
                useModule("dev.oom-wg.PureJoy-MultiLang:plugin:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://raw.githubusercontent.com/MetaCubeX/maven-backup/main/releases")
    }
}

plugins {
    id("com.highcapable.gropify") version "1.0.1"
}

gropify {
    isEnabled = true
    global {
        common {
            isEnabled = true
            useTypeAutoConversion = true
            useValueInterpolation = true
            existsPropertyFiles("gradle.properties", addDefault = false)
            excludeKeys(
                "signing.store.password",
                "signing.key.password",
                "signing.store.path",
                "signing.key.alias",
            )
        }
        android {
            generateDirPath = "build/generated/gropify"
            sourceSetName = "main"
            packageName = "com.github.yumelira.yumebox.yumebox.generated"
            useKotlin = true
            isRestrictedAccessEnabled = false
            isIsolationEnabled = true
        }
    }
    projects(":core", ":extension") {
        android { isEnabled = false }
    }
}

include(":core", ":extension", ":app")

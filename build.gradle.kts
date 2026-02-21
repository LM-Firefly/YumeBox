val isMergeBuild = System.getProperty("isMergeBuild") == "true"
extra.set("isMergeBuild", isMergeBuild)
tasks.register("assembleReleaseWithExtension") {
    group = "build"
    description = "Builds the app with the extension merged."
    dependsOn(":app:assembleRelease")
}
plugins {
    id("com.android.application") version "9.2.0-alpha01" apply false
    id("com.android.library") version "9.1.0-alpha09" apply false
    kotlin("plugin.serialization") version "2.3.10" apply false
    kotlin("plugin.compose") version "2.3.10" apply false
    id("org.jetbrains.compose") version "1.10.1" apply false
    id("com.google.devtools.ksp") version "2.3.5" apply false
    id("com.mikepenz.aboutlibraries.plugin") version "13.2.1" apply false
//    id("com.google.gms.google-services") version "4.4.4" apply false
//    id("com.google.firebase.crashlytics") version "3.0.6" apply false
    id("dev.oom-wg.purejoy.fyl.fytxt") version "+" apply false
}
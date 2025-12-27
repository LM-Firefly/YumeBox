val isMergeBuild = System.getProperty("isMergeBuild") == "true"
extra.set("isMergeBuild", isMergeBuild)
tasks.register("assembleReleaseWithExtension") {
    group = "build"
    description = "Builds the app with the extension merged."
    dependsOn(":app:assembleRelease")
}
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.aboutlibraries) apply false
    alias(libs.plugins.purejoy.mlang) apply false
    // Firebase plugins removed: com.google.gms.google-services and com.google.firebase.crashlytics
}
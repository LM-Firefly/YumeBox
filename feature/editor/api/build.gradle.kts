plugins {
    id("com.android.library")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

android {
    namespace = "com.github.yumelira.yumebox.feature.editor.api"
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
    sourceSets {
        getByName("main") {
            assets.directories.apply {
                clear()
                add("../assets")
            }
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":platform"))
    implementation(project(":locale"))
    implementation(project(":ui"))

    implementation(platform("io.github.rosemoe:editor-bom:${gropify.dep.version.soraEditor}"))
    implementation("io.github.rosemoe:editor")
    implementation("io.github.rosemoe:editor-lsp")
    implementation("io.github.rosemoe:language-textmate")
    implementation("io.github.rosemoe:language-treesitter")

    val composeBom = platform("androidx.compose:compose-bom:${gropify.dep.version.composeBom}")
    implementation(composeBom)
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:${gropify.dep.version.lifecycle}")
    implementation("com.jakewharton.timber:timber:${gropify.dep.version.timber}")
    implementation("top.yukonga.miuix.kmp:miuix-ui:${gropify.dep.version.miuix}")
}

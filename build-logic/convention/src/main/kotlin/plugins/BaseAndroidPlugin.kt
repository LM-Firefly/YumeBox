package plugins

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import core.ConfigProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import java.util.Properties

class BaseAndroidPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.withPlugin("com.android.application") { configureApp(target) }
        target.pluginManager.withPlugin("com.android.library") { configureLib(target) }
    }

    private fun configureApp(project: Project) {
        val provider = ConfigProvider(project)
        project.extensions.configure<ApplicationExtension> {
            val compileSdk = provider.getInt("android.compileSdk", 34)
            val minSdk = provider.getInt("android.minSdk", 24)
            val jvmVersion = provider.getString("android.jvm", provider.getString("project.jvm", "17"))

            this.compileSdk = compileSdk
            defaultConfig { this.minSdk = minSdk }
            compileOptions {
                val javaVersion = org.gradle.api.JavaVersion.toVersion(jvmVersion)
                sourceCompatibility = javaVersion
                targetCompatibility = javaVersion
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

            val signingFile = project.rootProject.file("signing.properties")
            if (signingFile.exists()) {
                val props = Properties()
                val loadResult = runCatching {
                    signingFile.inputStream().use(props::load)
                }
                if (loadResult.isFailure) {
                    project.logger.warn("[signing] Failed to load signing.properties: ${loadResult.exceptionOrNull()?.message}")
                    return@configure
                }

                val storePath = props.getProperty("storeFile")
                    ?: props.getProperty("signing.store.path")
                    ?: props.getProperty("keystore.path")
                val storePassword = props.getProperty("storePassword")
                    ?: props.getProperty("signing.store.password")
                    ?: props.getProperty("keystore.password")
                val keyAlias = props.getProperty("keyAlias")
                    ?: props.getProperty("signing.key.alias")
                    ?: props.getProperty("key.alias")
                val keyPassword = props.getProperty("keyPassword")
                    ?: props.getProperty("signing.key.password")
                    ?: props.getProperty("key.password")

                if (storePath.isNullOrBlank() || storePassword.isNullOrBlank() || keyAlias.isNullOrBlank() || keyPassword.isNullOrBlank()) {
                    project.logger.warn("[signing] Incomplete signing.properties: require storePath/storePassword/keyAlias/keyPassword")
                    return@configure
                }

                val keyStoreFile = project.rootProject.file(storePath)
                if (!keyStoreFile.exists()) {
                    project.logger.warn("[signing] Keystore path not found: ${keyStoreFile.absolutePath}")
                    return@configure
                }

                signingConfigs {
                    if (signingConfigs.findByName("release") == null) {
                        create("release") {
                            storeFile = keyStoreFile
                            this.storePassword = storePassword
                            this.keyAlias = keyAlias
                            this.keyPassword = keyPassword
                        }
                    }
                }
                buildTypes.configureEach {
                    if (name == "release") {
                        signingConfig = signingConfigs.getByName("release")
                    }
                }
            }
        }
    }

    private fun configureLib(project: Project) {
        val provider = ConfigProvider(project)
        project.extensions.configure<LibraryExtension> {
            val compileSdk = provider.getInt("android.compileSdk", 34)
            val minSdk = provider.getInt("android.minSdk", 24)
            val jvmVersion = provider.getString("android.jvm", provider.getString("project.jvm", "17"))
            val ndkVersionStr = provider.getString("android.ndkVersion", "")

            this.compileSdk = compileSdk
            if (ndkVersionStr.isNotBlank()) {
                ndkVersion = ndkVersionStr
            }
            defaultConfig { this.minSdk = minSdk }
            compileOptions {
                val javaVersion = org.gradle.api.JavaVersion.toVersion(jvmVersion)
                sourceCompatibility = javaVersion
                targetCompatibility = javaVersion
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
    }
}

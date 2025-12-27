package plugins

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import core.ConfigProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

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
            val jvmVersionStr = provider.getString("android.jvm", provider.getString("project.jvm", ""))
            val jvmVersionInt = runCatching { jvmVersionStr.toInt() }.getOrDefault(21)
            val javaVersionInt = minOf(jvmVersionInt, 21)
            val javaVersion = org.gradle.api.JavaVersion.toVersion(javaVersionInt.toString())

            this.compileSdk = compileSdk
            defaultConfig { this.minSdk = minSdk }
            compileOptions {
                sourceCompatibility = javaVersion
                targetCompatibility = javaVersion
            }

            val toolchainService = project.extensions.getByType(JavaToolchainService::class.java)
            project.tasks.withType(JavaCompile::class.java).configureEach {
                javaCompiler.set(toolchainService.compilerFor {
                    languageVersion.set(JavaLanguageVersion.of(javaVersionInt))
                })
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
                try {
                    val props = java.util.Properties().apply { load(signingFile.inputStream()) }
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
                    if (!storePath.isNullOrBlank() && !storePassword.isNullOrBlank() && !keyAlias.isNullOrBlank() && !keyPassword.isNullOrBlank()) {
                        val ks = project.rootProject.file(storePath)
                        if (!ks.exists()) {
                            project.logger.warn("[signing] Keystore path not found: ${ks.absolutePath}")
                        } else {
                            signingConfigs {
                                val releaseCfg = signingConfigs.findByName("release") ?: create("release") {
                                    storeFile = ks
                                    this.storePassword = storePassword
                                    this.keyAlias = keyAlias
                                    this.keyPassword = keyPassword
                                }
                                project.logger.lifecycle("[signing] Using keystore '${releaseCfg.storeFile?.name}' for release signing")
                            }
                            buildTypes.configureEach {
                                if (name == "release") {
                                    signingConfig = signingConfigs.getByName("release")
                                    project.logger.lifecycle("[signing] Applied signing to buildType 'release' in project ${project.path}")
                                }
                            }
                        }
                    } else {
                        project.logger.warn("[signing] Incomplete signing.properties (storePath=$storePath, storePassword=$storePassword, keyAlias=$keyAlias, keyPassword=$keyPassword)")
                    }
                } catch (e: Exception) {
                    project.logger.warn("[signing] Failed to load signing.properties: ${e.message}")
                }
            } else {
                project.logger.lifecycle("[signing] signing.properties not found; release builds will be unsigned.")
            }
            configureKotlinJvm(project, javaVersionInt)
        }
    }

    private fun configureLib(project: Project) {
        val provider = ConfigProvider(project)
        project.extensions.configure<LibraryExtension> {
            val compileSdk = provider.getInt("android.compileSdk", 34)
            val minSdk = provider.getInt("android.minSdk", 24)
            val jvmVersionStr = provider.getString("android.jvm", provider.getString("project.jvm", ""))
            val jvmVersionInt = runCatching { jvmVersionStr.toInt() }.getOrDefault(21)
            val javaVersionInt = minOf(jvmVersionInt, 21)
            val ndkVersionStr = provider.getString("android.ndkVersion", "")

            this.compileSdk = compileSdk
            if (ndkVersionStr.isNotBlank()) {
                ndkVersion = ndkVersionStr
            }
            defaultConfig { this.minSdk = minSdk }
            compileOptions {
                val javaVersion = org.gradle.api.JavaVersion.toVersion(javaVersionInt.toString())
                sourceCompatibility = javaVersion
                targetCompatibility = javaVersion
            }
            val toolchainService = project.extensions.getByType(JavaToolchainService::class.java)
            project.tasks.withType(JavaCompile::class.java).configureEach {
                javaCompiler.set(toolchainService.compilerFor {
                    languageVersion.set(JavaLanguageVersion.of(javaVersionInt))
                })
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
            configureKotlinJvm(project, javaVersionInt)
        }
    }

    private fun configureKotlinJvm(project: Project, javaVersionInt: Int) {
        listOf(
            org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile::class.java,
            org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java
        ).forEach { taskClass ->
            project.tasks.withType(taskClass).configureEach {
                try {
                    val method = this::class.java.methods.firstOrNull { it.name == "getCompilerOptions" }
                    if (method != null) {
                        val compilerOptions = method.invoke(this)
                        val setJvmTarget = compilerOptions::class.java.methods.firstOrNull { it.name == "setJvmTarget" && it.parameterTypes.size == 1 && it.parameterTypes[0] == String::class.java }
                        if (setJvmTarget != null) {
                            setJvmTarget.invoke(compilerOptions, javaVersionInt.toString())
                        } else {
                            try {
                                (this as? org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile)?.compilerOptions?.jvmTarget?.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(javaVersionInt.toString()))
                            } catch (_: Throwable) {
                                // no-op
                            }
                        }
                    } else {
                        try {
                            (this as? org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile)?.compilerOptions?.jvmTarget?.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(javaVersionInt.toString()))
                        } catch (_: Throwable) {
                            // no-op
                        }
                    }
                } catch (e: Throwable) {
                    try {
                        val kotlinOptionsGetter = this::class.java.methods.firstOrNull { it.name == "getKotlinOptions" }
                            ?: this::class.java.methods.firstOrNull { it.name == "kotlinOptions" }
                        val kotlinOptions = kotlinOptionsGetter?.invoke(this) ?: return@configureEach
                        val setJvm = kotlinOptions::class.java.methods.firstOrNull { m ->
                            m.name == "setJvmTarget" && m.parameterTypes.size == 1 && m.parameterTypes[0] == String::class.java
                        }
                        setJvm?.invoke(kotlinOptions, javaVersionInt.toString())
                    } catch (_: Throwable) {
                        // no-op: best-effort compatibility
                    }
                }
                doFirst {
                    try {
                        val method = this::class.java.methods.firstOrNull { it.name == "getCompilerOptions" }
                        if (method != null) {
                            val compilerOptions = method.invoke(this)
                            val setJvmTarget = compilerOptions::class.java.methods.firstOrNull { it.name == "setJvmTarget" && it.parameterTypes.size == 1 && it.parameterTypes[0] == String::class.java }
                            setJvmTarget?.invoke(compilerOptions, javaVersionInt.toString())
                        } else {
                            try {
                                (this as? org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile)?.compilerOptions?.jvmTarget?.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(javaVersionInt.toString()))
                            } catch (_: Throwable) {
                                // no-op
                            }
                        }
                    } catch (e: Throwable) {
                        try {
                            val kotlinOptionsGetter = this::class.java.methods.firstOrNull { it.name == "getKotlinOptions" }
                                ?: this::class.java.methods.firstOrNull { it.name == "kotlinOptions" }
                            val kotlinOptions = kotlinOptionsGetter?.invoke(this) ?: return@doFirst
                            val setJvm = kotlinOptions::class.java.methods.firstOrNull { m ->
                                m.name == "setJvmTarget" && m.parameterTypes.size == 1 && m.parameterTypes[0] == String::class.java
                            }
                            setJvm?.invoke(kotlinOptions, javaVersionInt.toString())
                        } catch (_: Throwable) {
                            // no-op
                        }
                    }
                }
            }
        }
    }
}

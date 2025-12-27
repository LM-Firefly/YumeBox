package plugins

import core.ConfigProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

class GolangConfigPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val provider = ConfigProvider(target)
        val ext = target.extensions.create<GolangExtension>("golang")
        ext.apply {
            sourceDir.convention(target.layout.projectDirectory.dir("src/golang/native"))
            outputDir.convention(target.layout.buildDirectory.dir("golang"))
            architectures.convention(GolangExtension.DEFAULT_ARCHITECTURES)
            buildTags.convention(
                provider.getCsv(
                    "golang.buildTags",
                    GolangExtension.DEFAULT_BUILD_TAGS.joinToString(","),
                ).toMutableList(),
            )
            buildFlags.convention(
                provider.getCsv(
                    "golang.buildFlags",
                    GolangExtension.DEFAULT_BUILD_FLAGS.joinToString(","),
                ).toMutableList(),
            )
        }
    }
}

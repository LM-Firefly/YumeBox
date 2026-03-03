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
 * Copyright (c)  YumeLira 2025 - Present
 *
 */

package plugins

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import core.applyBaseAndroidConvention
import core.applyReleaseSigningFrom
import core.readAndroidConventionValues
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class BaseAndroidPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val values = target.readAndroidConventionValues()
        target.pluginManager.withPlugin("com.android.application") { configureApp(target, values) }
        target.pluginManager.withPlugin("com.android.library") { configureLib(target, values) }
    }

    private fun configureApp(
        project: Project,
        values: core.AndroidConventionValues,
    ) {
        project.extensions.configure<ApplicationExtension> {
            applyBaseAndroidConvention(values)
            applyReleaseSigningFrom(project)
        }
    }

    private fun configureLib(
        project: Project,
        values: core.AndroidConventionValues,
    ) {
        project.extensions.configure<LibraryExtension> {
            applyBaseAndroidConvention(values)
        }
    }
}

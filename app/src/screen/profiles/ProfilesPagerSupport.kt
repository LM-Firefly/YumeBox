package com.github.yumelira.yumebox.screen.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.App
import com.github.yumelira.yumebox.presentation.component.AppDialog
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File

internal fun openProfileConfigPreview(
    targetFile: File,
    missingMessage: String,
    editable: Boolean,
    onReadFailed: (String) -> Unit,
    onPreviewPrepared: (String, ((String) -> Unit)?) -> Unit,
) {
    if (!targetFile.exists()) {
        onReadFailed(missingMessage)
        return
    }

    val configContent = runCatching { targetFile.readText() }.getOrElse {
        onReadFailed(it.message ?: "Failed to read profile")
        return
    }

    val saveCallback = if (editable) {
        { updatedContent: String ->
            runCatching {
                targetFile.writeText(updatedContent)
            }
                .getOrElse {
                    throw IllegalStateException(it.message ?: MLang.ProfilesPage.SettingsDialog.SaveFailed, it)
                }
        }
    } else {
        null
    }

    onPreviewPrepared(configContent, saveCallback)
}

@Composable
internal fun ProfileEditOptionsDialog(
    show: Boolean,
    onOpenConfig: () -> Unit,
    onEditSettings: () -> Unit,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
) {
    AppDialog(
        show = show,
        title = MLang.ProfilesPage.SettingsDialog.EditProfile,
        onDismissRequest = onDismiss,
        onDismissFinished = onDismissFinished,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenConfig,
            ) {
                Text(MLang.ProfilesPage.SettingsDialog.OpenConfig)
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onEditSettings,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(
                    text = MLang.ProfilesPage.SettingsDialog.EditSettings,
                    color = MiuixTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

internal fun importedProfileDir(profile: Profile): File {
    return App.instance.filesDir.resolve("imported").resolve(profile.uuid.toString())
}

internal fun importedConfigFile(profile: Profile): File {
    return importedProfileDir(profile).resolve("config.yaml")
}

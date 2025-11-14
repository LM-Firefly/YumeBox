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

package com.github.yumelira.yumebox.feature.meta.presentation.component

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.github.yumelira.yumebox.runtime.client.AppIdentityResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ConnectionLeadingIcon(
    metadata: JsonObject,
    network: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    bitmapSize: Int = 80,
) {
    val context = LocalContext.current
    val appIdentityResolver = remember(context) { AppIdentityResolver(context) }
    val identity = remember(metadata, appIdentityResolver) {
        appIdentityResolver.resolve(metadata)
    }
    val iconKey = remember(identity, bitmapSize) {
        "${identity.appKey}|${identity.packageName.orEmpty()}|$bitmapSize"
    }
    val iconBitmap by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = iconKey,
    ) {
        value = withContext(Dispatchers.IO) {
            ConnectionAppIconResolver.resolveIcon(
                context = context,
                packageName = identity.packageName,
                bitmapSize = bitmapSize,
            )
        }
    }

    val bitmap = iconBitmap
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = identity.appName.ifEmpty { network },
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape((size.value * 0.32f).dp)),
        )
        return
    }

    ProtocolFallbackIcon(
        network = network,
        modifier = modifier,
        size = size,
    )
}

@Composable
private fun ProtocolFallbackIcon(
    network: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val neutral = MiuixTheme.colorScheme.onSurface
    val protocolColor = getProtocolColor(network)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape((size.value * 0.32f).dp))
            .background(neutral.copy(alpha = 0.06f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = network.take(3).uppercase(),
            style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
            color = protocolColor,
        )
    }
}

private object ConnectionAppIconResolver {
    fun resolveIcon(
        context: Context,
        packageName: String?,
        bitmapSize: Int,
    ): ImageBitmap? {
        val resolvedPackageName = packageName?.trim().orEmpty().takeIf { it.isNotEmpty() } ?: return null
        return runCatching {
            context.packageManager
                .getApplicationIcon(resolvedPackageName)
                .toBitmap(width = bitmapSize, height = bitmapSize)
                .asImageBitmap()
        }.getOrNull()
    }
}

internal fun getProtocolColor(network: String) = when (network.uppercase()) {
    "TCP" -> androidx.compose.ui.graphics.Color(0xFF2196F3)
    "UDP" -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
    "HTTP" -> androidx.compose.ui.graphics.Color(0xFF9E9E9E)
    "HTTPS" -> androidx.compose.ui.graphics.Color(0xFF00BCD4)
    else -> androidx.compose.ui.graphics.Color(0xFF9E9E9E)
}

package com.github.yumelira.yumebox.presentation.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun NodeInfoDisplay(
    serverName: String?,
    serverPing: Int?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = MLang.Home.NodeInfo.Node,
                style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = serverName ?: "Not Selected",
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = MLang.Home.NodeInfo.Delay,
                style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            Spacer(modifier = Modifier.height(4.dp))
            PingValue(ping = serverPing)
        }
    }
}

@Composable
private fun PingValue(ping: Int?) {
    if (ping != null && ping <= 1000) {
        val color = if (ping < 500) {
            Color(0xFF4CAF50)
        } else {
            Color(0xFFFFA726)
        }
        Text(
            text = "$ping",
            style = MiuixTheme.textStyles.body1,
            color = color
        )
    } else {
        Text(
            text = "--",
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
    }
}

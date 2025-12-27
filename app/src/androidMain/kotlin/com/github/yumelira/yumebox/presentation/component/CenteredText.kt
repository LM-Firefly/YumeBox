package com.github.yumelira.yumebox.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun CenteredText(
    firstLine: String,
    secondLine: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = firstLine,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = secondLine,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}
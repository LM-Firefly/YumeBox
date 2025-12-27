package com.github.yumelira.yumebox.presentation.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.common.AppConstants
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HomeIdleContent(
    oneWord: String,
    author: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 100.dp),
        verticalArrangement = Arrangement.spacedBy(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "\"$oneWord\"",
            style = MiuixTheme.textStyles.headline1.copy(
                fontSize = AppConstants.UI.QUOTE_FONT_SIZE,
                lineHeight = AppConstants.UI.QUOTE_LINE_HEIGHT,
                letterSpacing = 1.sp
            ),
            color = MiuixTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = "— $author",
            style = MiuixTheme.textStyles.title2.copy(
                fontSize = AppConstants.UI.AUTHOR_FONT_SIZE
            ),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
    }
}

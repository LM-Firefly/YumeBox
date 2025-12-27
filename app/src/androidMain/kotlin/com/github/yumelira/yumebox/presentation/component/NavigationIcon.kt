package com.github.yumelira.yumebox.presentation.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.dropUnlessResumed
import com.github.yumelira.yumebox.presentation.theme.AppTheme.spacing
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun NavigationBackIcon(
    navigator: DestinationsNavigator,
    modifier: Modifier = Modifier,
    contentDescription: String = MLang.Component.Navigation.Back,
) {
    IconButton(
        modifier = modifier.padding(start = spacing.xl),
        onClick = dropUnlessResumed { navigator.popBackStack() },
    ) {
        Icon(
            imageVector = MiuixIcons.Back,
            contentDescription = contentDescription,
            tint = colorScheme.onBackground,
        )
    }
}

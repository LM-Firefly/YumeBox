package com.github.yumelira.yumebox.screen.about

import androidx.compose.runtime.Composable
import com.github.yumelira.yumebox.lite.BuildConfig
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.LinkItem
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.Title
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.component.combinePaddingValues
import com.github.yumelira.yumebox.presentation.component.rememberStandalonePageMainPadding
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold

@Destination<RootGraph>
@Composable
fun AboutScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            TopBar(title = "关于 Lite", scrollBehavior = scrollBehavior)
        },
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
        ) {
            item {
                Title("版本")
                Card {
                    BasicComponent(
                        title = "YumeBox Lite",
                        summary = BuildConfig.VERSION_NAME,
                    )
                }

                Title("链接")
                Card {
                    LinkItem(
                        title = "源码",
                        url = "https://github.com/YumeLira/YumeBox",
                    )
                    LinkItem(
                        title = "许可证",
                        url = "https://github.com/YumeLira/YumeBox/blob/main/LICENSE",
                    )
                    LinkItem(
                        title = "隐私政策",
                        url = "https://github.com/YumeLira/YumeBox/blob/main/PRIVACY_POLICY.md",
                    )
                }
            }
        }
    }
}

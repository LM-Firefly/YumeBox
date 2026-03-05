package com.github.yumelira.yumebox.di

import com.github.yumelira.yumebox.presentation.viewmodel.OverrideViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureOverrideViewModelModule = module {
    viewModel { OverrideViewModel(get()) }
}

val featureOverrideModules = listOf(
    featureOverrideViewModelModule,
)

package com.github.yumelira.yumebox.presentation.icon

import androidx.compose.ui.graphics.vector.ImageVector
import com.github.yumelira.yumebox.presentation.icon.yume.`Badge-plus`
import com.github.yumelira.yumebox.presentation.icon.yume.`Circle-fading-arrow-up`
import kotlin.String
import kotlin.collections.List as ____KtList
import kotlin.collections.Map as ____KtMap

public object Yume

private var __AllIcons: ____KtList<ImageVector>? = null

public val Yume.AllIcons: ____KtList<ImageVector>
  get() {
    if (__AllIcons != null) {
      return __AllIcons!!
    }
    __AllIcons= listOf(`Badge-plus`, `Circle-fading-arrow-up`)
    return __AllIcons!!
  }

private var __AllIconsNamed: ____KtMap<String, ImageVector>? = null

public val Yume.AllIconsNamed: ____KtMap<String, ImageVector>
  get() {
    if (__AllIconsNamed != null) {
      return __AllIconsNamed!!
    }
    __AllIconsNamed= mapOf("badge-plus" to `Badge-plus`, "circle-fading-arrow-up" to
        `Circle-fading-arrow-up`)
    return __AllIconsNamed!!
  }

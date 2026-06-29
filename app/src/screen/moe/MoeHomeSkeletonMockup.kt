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
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

package com.github.yumelira.yumebox.screen.moe

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Which guided demo the home skeleton mockup plays. */
internal enum class HomeMockupDemo {
    /** The bottom-right "start" chip is highlighted and taps in a repeating rhythm. */
    StartButton,

    /** A plain start chip; instead the hero is tapped and a photo-picker sheet slides up. */
    Wallpaper,
}

/**
 * A miniature, purely decorative "skeleton" mockup of the Moe home page drawn entirely from masked
 * placeholder blocks (no real data): a phone frame with a left sidebar (time + nav dots) and a right
 * content panel (hero block, a faux node-info strip, quote bars). In [HomeMockupDemo.StartButton] the
 * bottom-right "start" chip is highlighted — it pulses with a radiating ripple to draw the eye. In
 * [HomeMockupDemo.Wallpaper] the chip is plain; instead the hero plays a looping tap effect and a
 * photo-picker bottom sheet slides up. Purely decorative, never interactive.
 */
@Composable
internal fun MoeHomeSkeletonMockup(
    demo: HomeMockupDemo = HomeMockupDemo.StartButton,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme
    val opacity = AppTheme.opacity

    val mask = colorScheme.onSurface.copy(alpha = opacity.subtle)
    val maskStrong = colorScheme.onSurface.copy(alpha = opacity.subtleStrong)
    val onHero = colorScheme.surface.copy(alpha = opacity.secondaryText)
    val frameBorder = colorScheme.outline.copy(alpha = opacity.surfaceSoft)

    val wallpaper = demo == HomeMockupDemo.Wallpaper

    // The wallpaper demo drives a single looped cycle: tap the hero, then raise the photo picker.
    val transition = rememberInfiniteTransition(label = "home_wallpaper")
    val sheetProgress by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        keyframes {
                            durationMillis = 3600
                            0f at 0
                            0f at 900
                            1f at 1300 with FastOutSlowInEasing
                            1f at 2900
                            0f at 3300 with FastOutSlowInEasing
                            0f at 3600
                        },
                    repeatMode = RepeatMode.Restart,
                ),
            label = "wallpaper_sheet_progress",
        )
    val heroTapScale by
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        keyframes {
                            durationMillis = 3600
                            1f at 0
                            1f at 700
                            0.85f at 850 with FastOutSlowInEasing
                            1f at 1000
                            1f at 3600
                        },
                    repeatMode = RepeatMode.Restart,
                ),
            label = "wallpaper_hero_tap_scale",
        )
    val heroRippleScale by
        transition.animateFloat(
            initialValue = 0.6f,
            targetValue = 2.4f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "wallpaper_hero_ripple_scale",
        )
    val heroRippleAlpha by
        transition.animateFloat(
            initialValue = 0.5f,
            targetValue = 0f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "wallpaper_hero_ripple_alpha",
        )

    Box(
        modifier =
            modifier
                .fillMaxWidth(0.64f)
                .aspectRatio(0.46f)
                .clip(RoundedCornerShape(22.dp))
                .background(colorScheme.surface)
                .border(1.5.dp, frameBorder, RoundedCornerShape(22.dp))
                .padding(7.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left sidebar (~22%): vertical clock near the top, nav icon rail pinned to the bottom.
            Column(
                modifier = Modifier.fillMaxHeight().weight(0.22f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(10.dp))
                // Vertical clock: one continuous stroke ("一笔带过").
                Box(
                    Modifier.width(13.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(maskStrong)
                )

                Spacer(Modifier.weight(1f))

                // Nav icon rail: three icons stacked near the bottom.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    repeat(3) {
                        Box(Modifier.size(13.dp).clip(RoundedCornerShape(4.dp)).background(mask))
                    }
                }

                Spacer(Modifier.height(10.dp))
            }

            // Right content (~78%): hero (traffic + node info) + quote + highlighted start chip.
            Column(
                modifier = Modifier.fillMaxHeight().weight(0.78f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .weight(2f)
                            .clip(RoundedCornerShape(13.dp))
                            .background(maskStrong)
                ) {
                    Column(
                        modifier =
                            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(7.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // Traffic strip: UP on the left, DOWN on the right.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Box(
                                Modifier.width(30.dp)
                                    .height(9.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(onHero)
                            )
                            Box(
                                Modifier.width(30.dp)
                                    .height(9.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(onHero)
                            )
                        }
                        // Node info: flag circle + node name on the left, ping on the right.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(onHero))
                                Box(
                                    Modifier.width(46.dp)
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(onHero)
                                )
                            }
                            Box(
                                Modifier.width(20.dp)
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(onHero)
                            )
                        }
                    }

                    // Wallpaper demo: a looping tap effect centered on the hero, just before the
                    // photo-picker sheet rises.
                    if (wallpaper) {
                        Box(
                            modifier = Modifier.align(Alignment.Center),
                            contentAlignment = Alignment.Center,
                        ) {
                            // Ripple ring radiating from the touch point.
                            Box(
                                modifier =
                                    Modifier.graphicsLayer {
                                            scaleX = heroRippleScale
                                            scaleY = heroRippleScale
                                            alpha = heroRippleAlpha
                                        }
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(colorScheme.primary)
                            )
                            // Touch dot pressing on each tap.
                            Box(
                                modifier =
                                    Modifier.graphicsLayer {
                                            scaleX = heroTapScale
                                            scaleY = heroTapScale
                                        }
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(colorScheme.surface)
                                        .border(2.dp, colorScheme.primary, CircleShape)
                            )
                        }
                    }
                }

                // Bottom third: quote block + a right-aligned highlighted start chip.
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Quote block: three left-aligned lines + a right-aligned author line.
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            Modifier.fillMaxWidth(0.92f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(mask)
                        )
                        Box(
                            Modifier.fillMaxWidth(0.8f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(mask)
                        )
                        Box(
                            Modifier.fillMaxWidth(0.5f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(mask)
                        )
                        Box(
                            Modifier.align(Alignment.End)
                                .fillMaxWidth(0.3f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(mask)
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        if (wallpaper) PlainStartChip() else HighlightedStartChip()
                    }
                }
            }
        }

        // Wallpaper demo: dim scrim + the photo-picker bottom sheet sliding up. Clipped to the
        // phone-frame's inner radius so they follow the rounded corners instead of sharp edges.
        if (wallpaper) {
            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(15.dp))) {
                Box(
                    modifier =
                        Modifier.matchParentSize()
                            .background(Color.Black.copy(alpha = 0.40f * sheetProgress))
                )
                PhotoPickerSheet(
                    sheetProgress = sheetProgress,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

/**
 * The highlighted "start" chip: a primary capsule that visibly "presses" in a repeating finger-tap
 * rhythm, wrapped by a radiating ripple ring and topped with a prominent "touch" dot — evoking a
 * deliberate tap on the connect button. Decorative only, never clickable.
 */
@Composable
private fun HighlightedStartChip(modifier: Modifier = Modifier) {
    val colorScheme = MiuixTheme.colorScheme

    val transition = rememberInfiniteTransition(label = "start_chip")
    // A press that reads as a tap: hold, snap down, snap back, then rest until the next cycle.
    val pressScale by
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        keyframes {
                            durationMillis = 1500
                            1f at 0
                            1f at 200
                            0.9f at 360 with FastOutSlowInEasing
                            1f at 560
                            1f at 1500
                        },
                    repeatMode = RepeatMode.Restart,
                ),
            label = "press_scale",
        )
    val rippleScale by
        transition.animateFloat(
            initialValue = 0.6f,
            targetValue = 2.6f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "ripple_scale",
        )
    val rippleAlpha by
        transition.animateFloat(
            initialValue = 0.5f,
            targetValue = 0f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "ripple_alpha",
        )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Ripple ring radiating from behind the touch dot.
        Box(
            modifier =
                Modifier.align(Alignment.BottomEnd)
                    .offset(x = 6.dp, y = 6.dp)
                    .graphicsLayer {
                        scaleX = rippleScale
                        scaleY = rippleScale
                        alpha = rippleAlpha
                    }
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primary)
        )

        // Primary capsule that presses on each tap.
        Box(
            modifier =
                Modifier.graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    }
                    .clip(RoundedCornerShape(50))
                    .background(colorScheme.primary)
                    .padding(horizontal = 18.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = MLang.Home.Control.Start,
                color = colorScheme.onPrimary,
                style =
                    MiuixTheme.textStyles.footnote1.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
            )
        }

        // Prominent touch dot overlapping the chip's right end, pressing in sync with the capsule.
        Box(
            modifier =
                Modifier.align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    }
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primary)
                    .border(2.dp, colorScheme.surface, CircleShape)
        )
    }
}

/**
 * A plain, non-highlighted "start" chip: the same primary capsule and "启动" label as
 * [HighlightedStartChip] but static — no ripple, touch dot, or pulse. Used while the wallpaper demo
 * draws the eye to the hero instead. Decorative only.
 */
@Composable
private fun PlainStartChip(modifier: Modifier = Modifier) {
    val colorScheme = MiuixTheme.colorScheme

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(50))
                .background(colorScheme.primary)
                .padding(horizontal = 18.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = MLang.Home.Control.Start,
            color = colorScheme.onPrimary,
            style =
                MiuixTheme.textStyles.footnote1.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
        )
    }
}

/**
 * A miniature mock of the system "安全访问" album picker bottom sheet: a drag handle, highlighted
 * "全部" + "影集" tab pills, a notice bar, and a 3×3 photo grid that fills the sheet, whose first tile
 * suggests the camera. Slides up from the bottom driven by [sheetProgress]. Decorative only.
 */
@Composable
private fun PhotoPickerSheet(sheetProgress: Float, modifier: Modifier = Modifier) {
    val colorScheme = MiuixTheme.colorScheme
    val opacity = AppTheme.opacity

    val mask = colorScheme.onSurface.copy(alpha = opacity.subtle)
    val maskStrong = colorScheme.onSurface.copy(alpha = opacity.subtleStrong)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .graphicsLayer { translationY = (1f - sheetProgress) * size.height }
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(colorScheme.surface)
                .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Drag handle.
            Box(
                Modifier.width(22.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(mask)
            )

            // Tab pills: "全部" highlighted, "影集" muted.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier.weight(1f)
                        .height(15.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(colorScheme.primary.copy(alpha = opacity.subtleStrong))
                )
                Box(
                    Modifier.weight(1f)
                        .height(15.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(mask)
                )
            }

            // Notice bar.
            Box(
                Modifier.fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(mask)
            )

            // Photo grid: 3 rows of 3 tiles filling the freed space; the first tile is the camera.
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(3) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        repeat(3) { col ->
                            val isCamera = row == 0 && col == 0
                            Box(
                                modifier =
                                    Modifier.weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (isCamera) mask else maskStrong),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isCamera) {
                                    Box(
                                        Modifier.size(6.dp)
                                            .clip(CircleShape)
                                            .background(maskStrong)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A miniature, purely decorative skeleton of the Profiles / Subscriptions page: a top bar with a title
 * and a highlighted "+" add button, a short list of profile cards, and a looping demo where the add
 * button is tapped and the "add profile" bottom sheet slides up from the bottom. Never interactive.
 */
@Composable
internal fun ProfilesSkeletonMockup(modifier: Modifier = Modifier) {
    val colorScheme = MiuixTheme.colorScheme
    val opacity = AppTheme.opacity

    val mask = colorScheme.onSurface.copy(alpha = opacity.subtle)
    val maskStrong = colorScheme.onSurface.copy(alpha = opacity.subtleStrong)
    val frameBorder = colorScheme.outline.copy(alpha = opacity.surfaceSoft)
    // Grey recessed input fields vs the elevated white type-selector card.
    val recessed = colorScheme.onSurface.copy(alpha = opacity.verySubtle)
    val card = colorScheme.surface

    val transition = rememberInfiniteTransition(label = "profiles_sheet")
    // Sheet stays hidden, slides up after the add tap, rests, then slides back down each cycle.
    val sheetProgress by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        keyframes {
                            durationMillis = 3600
                            0f at 0
                            0f at 900
                            1f at 1300 with FastOutSlowInEasing
                            1f at 2900
                            0f at 3300 with FastOutSlowInEasing
                            0f at 3600
                        },
                    repeatMode = RepeatMode.Restart,
                ),
            label = "sheet_progress",
        )

    Box(
        modifier =
            modifier
                .fillMaxWidth(0.64f)
                .aspectRatio(0.46f)
                .clip(RoundedCornerShape(22.dp))
                .background(colorScheme.surface)
                .border(1.5.dp, frameBorder, RoundedCornerShape(22.dp))
                .padding(7.dp)
    ) {
        // Base page: top bar + a column of weighted profile cards that fill the frame top to
        // bottom, with the floating bottom nav pill as the last child of the flow. Drawn directly
        // in the frame (only clipped by the outer 22.dp corner) so the top-right add button and its
        // expanding ripple are never cut by the inner overlay clip.
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Status-bar gap so the top bar (and the add button) don't sit flush against the frame.
            Spacer(Modifier.height(10.dp))

            // Top bar: bold "配置" title on the left, highlighted add button on the right.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.width(40.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(maskStrong)
                )
                AddButtonGlyph()
            }

            Spacer(Modifier.height(8.dp))

            // Profile cards: three cards that share and fill the vertical space so the page
            // reads full top-to-bottom (no empty white gap below the list).
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(3) { index ->
                    ProfileCardMock(
                        index = index,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Floating bottom nav pill (4 icons); the 3rd is the current page, highlighted.
            Row(
                modifier =
                    Modifier.clip(RoundedCornerShape(50))
                        .background(colorScheme.surface)
                        .border(1.dp, frameBorder, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(4) {
                    Box(
                        Modifier.size(9.dp)
                            .clip(CircleShape)
                            .background(if (it == 2) colorScheme.primary else mask)
                    )
                }
            }
        }

        // Overlay only: dim scrim + the "add profile" bottom sheet, clipped to the phone-frame's
        // inner radius so the dim mask follows the rounded corners instead of sharp edges. The base
        // page above is intentionally left outside this clip.
        Box(modifier = Modifier.matchParentSize().clip(RoundedCornerShape(15.dp))) {
            // Dim scrim, visible only while the sheet is up.
            Box(
                modifier =
                    Modifier.matchParentSize()
                        .background(Color.Black.copy(alpha = 0.40f * sheetProgress))
            )

            // The "add profile" bottom sheet sliding up from the bottom.
            Box(
                modifier =
                    Modifier.align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.62f)
                        .graphicsLayer { translationY = (1f - sheetProgress) * size.height }
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(colorScheme.surface)
                        .border(
                            1.dp,
                            frameBorder,
                            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        )
                        .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Drag handle.
                    Box(
                        Modifier.width(26.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(mask)
                    )
                    // Title row: close ✕ on the left, title block centered, confirm ✓ on the right.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(11.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(maskStrong)
                        )
                        Box(
                            Modifier.width(56.dp)
                                .height(11.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(maskStrong)
                        )
                        Box(
                            Modifier.size(11.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(maskStrong)
                        )
                    }
                    // Type-selector card: an elevated white card that stands out from the grey
                    // fields below — "配置类型" label on the left, "订阅链接" + chevrons on the right.
                    Box(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(card)
                            .border(1.dp, frameBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.width(46.dp)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(maskStrong)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier.width(34.dp)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(mask)
                                )
                                Box(
                                    Modifier.width(7.dp)
                                        .height(11.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(mask)
                                )
                            }
                        }
                    }
                    // Three recessed grey input fields: 配置名称 / 订阅链接 / Age 私钥(可选).
                    repeat(3) { index ->
                        Box(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .height(26.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(recessed)
                                    .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Box(
                                Modifier.fillMaxWidth(if (index == 2) 0.42f else 0.5f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(mask)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * One profile card in [ProfilesSkeletonMockup]. [index] 0 is a local card (toggle on, no info lines,
 * no 更新 chip); later cards are remote (toggle off, two info lines, a 更新 chip). The card stretches to
 * its given (weighted) height — a trailing [Spacer] pushes the divider + action row to the bottom so it
 * always reads full regardless of height. Decorative only.
 */
@Composable
private fun ProfileCardMock(index: Int, modifier: Modifier = Modifier) {
    val colorScheme = MiuixTheme.colorScheme
    val opacity = AppTheme.opacity

    val maskInner = colorScheme.onSurface.copy(alpha = opacity.subtleStrong)
    val card = colorScheme.surfaceVariant.copy(alpha = opacity.surfaceVariant)
    val frameBorder = colorScheme.outline.copy(alpha = opacity.surfaceSoft)

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(12.dp))
                .background(card)
                .border(1.dp, frameBorder, RoundedCornerShape(12.dp))
                .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Header: name + subtitle on the left, enable toggle on the right.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(
                        Modifier.width(40.dp)
                            .height(9.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(maskInner)
                    )
                    Box(
                        Modifier.width(26.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(maskInner)
                    )
                }
                // Pill toggle: green/on for the first card, grey/off otherwise.
                Box(
                    modifier =
                        Modifier.width(22.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (index == 0) colorScheme.primary else maskInner)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(1.5.dp),
                        contentAlignment =
                            if (index == 0) Alignment.CenterEnd else Alignment.CenterStart,
                    ) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(colorScheme.surface))
                    }
                }
            }

            // Remote cards show two info lines (流量 / 到期).
            if (index > 0) {
                Box(
                    Modifier.fillMaxWidth(0.85f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(maskInner)
                )
                Box(
                    Modifier.fillMaxWidth(0.7f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(maskInner)
                )
            }

            // Push the divider + action row to the bottom so the card looks full when stretched.
            Spacer(Modifier.weight(1f))

            // Thin divider above the action row.
            Box(
                Modifier.fillMaxWidth()
                    .height(1.dp)
                    .background(colorScheme.onSurface.copy(alpha = opacity.surfaceSoft))
            )

            // Action row: two circular icon buttons left, 更新/删除 chips right.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(maskInner))
                    Box(Modifier.size(14.dp).clip(CircleShape).background(maskInner))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (index > 0) {
                        // 更新 chip (green, remote only).
                        Box(
                            Modifier.width(26.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(colorScheme.primary.copy(alpha = opacity.subtleStrong))
                        )
                    }
                    // 删除 chip.
                    Box(
                        Modifier.width(26.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(maskInner)
                    )
                }
            }
        }
    }
}

/**
 * The highlighted "+" add button from the Profiles top bar: a primary rounded square with a drawn plus
 * sign that pulses and emits a ripple just before the demo bottom sheet appears. Decorative only.
 */
@Composable
private fun AddButtonGlyph(modifier: Modifier = Modifier) {
    val colorScheme = MiuixTheme.colorScheme

    val transition = rememberInfiniteTransition(label = "add_button")
    // A short tap right before the sheet rises.
    val tapScale by
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        keyframes {
                            durationMillis = 3600
                            1f at 0
                            1f at 700
                            0.85f at 850 with FastOutSlowInEasing
                            1f at 1000
                            1f at 3600
                        },
                    repeatMode = RepeatMode.Restart,
                ),
            label = "add_tap_scale",
        )
    val rippleScale by
        transition.animateFloat(
            initialValue = 0.6f,
            targetValue = 2.2f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "add_ripple_scale",
        )
    val rippleAlpha by
        transition.animateFloat(
            initialValue = 0.5f,
            targetValue = 0f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "add_ripple_alpha",
        )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Ripple ring radiating from behind the button.
        Box(
            modifier =
                Modifier.graphicsLayer {
                        scaleX = rippleScale
                        scaleY = rippleScale
                        alpha = rippleAlpha
                    }
                    .size(16.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(colorScheme.primary)
        )

        // Primary rounded square with a drawn "+".
        Box(
            modifier =
                Modifier.graphicsLayer {
                        scaleX = tapScale
                        scaleY = tapScale
                    }
                    .size(16.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.width(8.dp).height(2.dp).background(colorScheme.onPrimary))
            Box(Modifier.width(2.dp).height(8.dp).background(colorScheme.onPrimary))
        }
    }
}

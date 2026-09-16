@file:Suppress("compose:compositionlocal-allowlist")

package com.eric.contentfeed.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

@Immutable
data class SignalDimens(
    val space1: Dp = 4.dp,
    val space2: Dp = 8.dp,
    val space3: Dp = 12.dp,
    val space4: Dp = 16.dp,
    val space5: Dp = 24.dp,
    val space6: Dp = 32.dp,
    val space7: Dp = 40.dp,
    val touchMin: Dp = 48.dp,
    val iconSmall: Dp = 18.dp,
    val iconStandard: Dp = 24.dp,
    val thumbnailArticle: DpSize = DpSize(96.dp, 72.dp),
    val thumbnailSaved: DpSize = DpSize(112.dp, 84.dp),
    val detailImageHeight: Dp = 220.dp,
    val serviceImageHeight: Dp = 144.dp,
    val elevationNone: Dp = 0.dp,
    val elevationTonal1: Dp = 1.dp,
    val elevationTonal2: Dp = 3.dp,
    val divider: Dp = 1.dp,
)

val LocalSignalDimens =
    androidx.compose.runtime.staticCompositionLocalOf { SignalDimens() }

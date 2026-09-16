@file:Suppress("compose:compositionlocal-allowlist")

package com.eric.contentfeed.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class SignalExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
)

internal val SignalLightExtendedColors =
    SignalExtendedColors(
        success = Color(0xFF37694E),
        onSuccess = Color(0xFFFFFFFF),
        warning = Color(0xFF815E1F),
        onWarning = Color(0xFFFFFFFF),
    )

internal val SignalDarkExtendedColors =
    SignalExtendedColors(
        success = Color(0xFF9DDBB7),
        onSuccess = Color(0xFF07351F),
        warning = Color(0xFFEBC06C),
        onWarning = Color(0xFF3E2E00),
    )

val LocalSignalExtendedColors =
    staticCompositionLocalOf { SignalLightExtendedColors }

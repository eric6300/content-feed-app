@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

object ContentFeedTheme {
    val dimens: SignalDimens
        @Composable get() = LocalSignalDimens.current

    val extendedColors: SignalExtendedColors
        @Composable get() = LocalSignalExtendedColors.current

    @Composable
    operator fun invoke(
        darkTheme: Boolean = isSystemInDarkTheme(),
        content: @Composable () -> Unit,
    ) {
        CompositionLocalProvider(
            LocalSignalDimens provides SignalDimens(),
            LocalSignalExtendedColors provides
                if (darkTheme) SignalDarkExtendedColors else SignalLightExtendedColors,
        ) {
            MaterialTheme(
                colorScheme = if (darkTheme) SignalDarkColorScheme else SignalLightColorScheme,
                typography = SignalTypography,
                shapes = SignalShapes,
                content = content,
            )
        }
    }
}

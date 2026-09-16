package com.eric.contentfeed.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val SignalGround = Color(0xFFF4F6F3)
private val SignalSurface = Color(0xFFFCFDF9)
private val SignalSurfaceRaised = Color(0xFFFFFFFF)
private val SignalSurfaceMuted = Color(0xFFE8EEEC)
private val SignalInk = Color(0xFF18262B)
private val SignalInkMuted = Color(0xFF53646A)
private val SignalOutline = Color(0xFFC5D0D0)
private val SignalPrimary = Color(0xFF2E536C)
private val SignalOnPrimary = Color(0xFFFFFFFF)
private val SignalPrimaryContainer = Color(0xFFC7E8F5)
private val SignalOnPrimaryContainer = Color(0xFF0A2D3D)
private val SignalSecondary = Color(0xFF715A32)
private val SignalSecondaryContainer = Color(0xFFF1E1B9)
private val SignalOnSecondaryContainer = Color(0xFF2B210B)
private val SignalTertiary = Color(0xFFA24B36)
private val SignalTertiaryContainer = Color(0xFFF7D9CE)
private val SignalOnTertiaryContainer = Color(0xFF3F140B)
private val SignalError = Color(0xFFB3261E)
private val SignalOnError = Color(0xFFFFFFFF)
private val SignalErrorContainer = Color(0xFFF9DEDC)
private val SignalOnErrorContainer = Color(0xFF410E0B)

private val SignalDarkGround = Color(0xFF111719)
private val SignalDarkSurface = Color(0xFF171E20)
private val SignalDarkSurfaceRaised = Color(0xFF20282A)
private val SignalDarkSurfaceMuted = Color(0xFF273235)
private val SignalDarkInk = Color(0xFFE6F0F1)
private val SignalDarkInkMuted = Color(0xFFB7C6C8)
private val SignalDarkOutline = Color(0xFF3E4A4D)
private val SignalDarkPrimary = Color(0xFF9FD5E9)
private val SignalDarkOnPrimary = Color(0xFF003544)
private val SignalDarkPrimaryContainer = Color(0xFF124B5B)
private val SignalDarkOnPrimaryContainer = Color(0xFFC7E8F5)
private val SignalDarkSecondary = Color(0xFFDDC48D)
private val SignalDarkOnSecondary = Color(0xFF3D2F0F)
private val SignalDarkSecondaryContainer = Color(0xFF55461F)
private val SignalDarkOnSecondaryContainer = Color(0xFFF1E1B9)
private val SignalDarkTertiary = Color(0xFFFFB59E)
private val SignalDarkOnTertiary = Color(0xFF5B1D0E)
private val SignalDarkTertiaryContainer = Color(0xFF7F2F1F)
private val SignalDarkOnTertiaryContainer = Color(0xFFF7D9CE)
private val SignalDarkError = Color(0xFFFFB4AB)
private val SignalDarkOnError = Color(0xFF690005)
private val SignalDarkErrorContainer = Color(0xFF93000A)
private val SignalDarkOnErrorContainer = Color(0xFFFFDAD6)

internal val SignalLightColorScheme: ColorScheme =
    lightColorScheme(
        primary = SignalPrimary,
        onPrimary = SignalOnPrimary,
        primaryContainer = SignalPrimaryContainer,
        onPrimaryContainer = SignalOnPrimaryContainer,
        inversePrimary = SignalPrimaryContainer,
        secondary = SignalSecondary,
        onSecondary = SignalOnPrimary,
        secondaryContainer = SignalSecondaryContainer,
        onSecondaryContainer = SignalOnSecondaryContainer,
        tertiary = SignalTertiary,
        onTertiary = SignalOnPrimary,
        tertiaryContainer = SignalTertiaryContainer,
        onTertiaryContainer = SignalOnTertiaryContainer,
        background = SignalGround,
        onBackground = SignalInk,
        surface = SignalSurface,
        onSurface = SignalInk,
        surfaceVariant = SignalSurfaceMuted,
        onSurfaceVariant = SignalInkMuted,
        inverseSurface = SignalInk,
        inverseOnSurface = SignalSurface,
        error = SignalError,
        onError = SignalOnError,
        errorContainer = SignalErrorContainer,
        onErrorContainer = SignalOnErrorContainer,
        outline = SignalInkMuted,
        outlineVariant = SignalOutline,
        scrim = SignalInk,
        surfaceTint = SignalPrimary,
        surfaceBright = SignalSurface,
        surfaceDim = SignalGround,
        surfaceContainer = SignalSurfaceMuted,
        surfaceContainerHigh = SignalSurfaceMuted,
        surfaceContainerHighest = SignalSurfaceMuted,
        surfaceContainerLow = SignalSurface,
        surfaceContainerLowest = SignalSurfaceRaised,
    )

internal val SignalDarkColorScheme: ColorScheme =
    darkColorScheme(
        primary = SignalDarkPrimary,
        onPrimary = SignalDarkOnPrimary,
        primaryContainer = SignalDarkPrimaryContainer,
        onPrimaryContainer = SignalDarkOnPrimaryContainer,
        inversePrimary = SignalPrimary,
        secondary = SignalDarkSecondary,
        onSecondary = SignalDarkOnSecondary,
        secondaryContainer = SignalDarkSecondaryContainer,
        onSecondaryContainer = SignalDarkOnSecondaryContainer,
        tertiary = SignalDarkTertiary,
        onTertiary = SignalDarkOnTertiary,
        tertiaryContainer = SignalDarkTertiaryContainer,
        onTertiaryContainer = SignalDarkOnTertiaryContainer,
        background = SignalDarkGround,
        onBackground = SignalDarkInk,
        surface = SignalDarkSurface,
        onSurface = SignalDarkInk,
        surfaceVariant = SignalDarkSurfaceMuted,
        onSurfaceVariant = SignalDarkInkMuted,
        inverseSurface = SignalDarkInk,
        inverseOnSurface = SignalDarkSurface,
        error = SignalDarkError,
        onError = SignalDarkOnError,
        errorContainer = SignalDarkErrorContainer,
        onErrorContainer = SignalDarkOnErrorContainer,
        outline = SignalDarkInkMuted,
        outlineVariant = SignalDarkOutline,
        scrim = SignalDarkInk,
        surfaceTint = SignalDarkPrimary,
        surfaceBright = SignalDarkSurfaceRaised,
        surfaceDim = SignalDarkGround,
        surfaceContainer = SignalDarkSurfaceMuted,
        surfaceContainerHigh = SignalDarkSurfaceMuted,
        surfaceContainerHighest = SignalDarkSurfaceMuted,
        surfaceContainerLow = SignalDarkSurface,
        surfaceContainerLowest = SignalDarkSurfaceRaised,
    )

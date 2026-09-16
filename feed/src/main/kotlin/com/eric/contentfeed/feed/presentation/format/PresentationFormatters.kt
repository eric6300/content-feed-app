package com.eric.contentfeed.feed.presentation.format

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal fun formatPublishedDate(
    epochMillis: Long?,
    unknownLabel: String = "date unknown",
    locale: Locale = Locale.getDefault(),
): String {
    if (epochMillis == null) return unknownLabel
    return runCatching {
        DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(locale)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(epochMillis))
    }.getOrDefault(unknownLabel)
}

internal fun formatPrice(price: Double): String =
    NumberFormat
        .getNumberInstance(Locale.getDefault())
        .apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(price)

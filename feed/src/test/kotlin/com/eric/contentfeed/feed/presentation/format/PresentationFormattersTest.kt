package com.eric.contentfeed.feed.presentation.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneOffset
import java.util.Locale

class PresentationFormattersTest {
    @Test
    fun formatsMissingPublishedDateAsDateUnknown() {
        assertEquals("date unknown", formatPublishedDate(null, unknownLabel = "date unknown"))
    }

    @Test
    fun formatsPublishedEpochMillisAsLocalizedDate() {
        assertEquals(
            "Jan 1, 1970",
            formatPublishedDate(
                0,
                unknownLabel = "unknown",
                locale = Locale.US,
                timeZone = ZoneOffset.UTC,
            ),
        )
    }
}

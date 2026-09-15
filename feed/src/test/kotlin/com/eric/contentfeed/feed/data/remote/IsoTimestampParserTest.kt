package com.eric.contentfeed.feed.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IsoTimestampParserTest {
    @Test
    fun parsesAZSuffixedTimestamp() {
        assertEquals(1_757_869_761_000L, IsoTimestampParser.parseToEpochMillis("2025-09-14T17:09:21Z"))
    }

    @Test
    fun parsesAFractionalSecondTimestamp() {
        val millis = IsoTimestampParser.parseToEpochMillis("2025-09-14T17:09:21.811Z")
        assertEquals(1_757_869_761_811L, millis)
    }

    @Test
    fun parsesAnExplicitNumericOffsetTimestamp() {
        val utc = IsoTimestampParser.parseToEpochMillis("2025-09-14T17:09:21Z")
        val offset = IsoTimestampParser.parseToEpochMillis("2025-09-15T01:09:21+08:00")
        assertEquals(utc, offset)
    }

    @Test
    fun epochSentinelParsesToExactlyZero() {
        // The parser is purely technical — it does not know this is a sentinel.
        assertEquals(0L, IsoTimestampParser.parseToEpochMillis("1970-01-01T00:00:00Z"))
    }

    @Test
    fun nullInputReturnsNull() {
        assertNull(IsoTimestampParser.parseToEpochMillis(null))
    }

    @Test
    fun blankInputReturnsNull() {
        assertNull(IsoTimestampParser.parseToEpochMillis("   "))
    }

    @Test
    fun unparseableInputReturnsNull() {
        assertNull(IsoTimestampParser.parseToEpochMillis("not-a-date"))
    }
}

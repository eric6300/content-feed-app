package com.eric.contentfeed.core.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClickDebouncerTest {
    @Test
    fun firstClickIsAccepted() {
        val debouncer = ClickDebouncer(debounceMillis = 300L, uptimeMillis = { 1_000L })

        assertTrue(debouncer.shouldAccept())
    }

    @Test
    fun clickWithinWindowIsRejected() {
        var now = 1_000L
        val debouncer = ClickDebouncer(debounceMillis = 300L, uptimeMillis = { now })

        assertTrue(debouncer.shouldAccept())
        now += 100L
        assertFalse(debouncer.shouldAccept())
    }

    @Test
    fun clickAfterWindowIsAccepted() {
        var now = 1_000L
        val debouncer = ClickDebouncer(debounceMillis = 300L, uptimeMillis = { now })

        assertTrue(debouncer.shouldAccept())
        now += 300L
        assertTrue(debouncer.shouldAccept())
    }

    @Test
    fun sustainedRapidClicksDoNotExtendTheWindow() {
        var now = 1_000L
        val debouncer = ClickDebouncer(debounceMillis = 300L, uptimeMillis = { now })

        assertTrue(debouncer.shouldAccept())
        repeat(5) {
            now += 50L
            assertFalse(debouncer.shouldAccept())
        }
        now = 1_000L + 300L
        assertTrue(debouncer.shouldAccept())
    }
}

package com.eric.contentfeed.feed.data.remote

import java.time.OffsetDateTime

internal object IsoTimestampParser {
    /** Null only when the input is absent/blank or not a valid ISO-8601 offset
     * timestamp. `1970-01-01T00:00:00Z` legitimately parses to `0L` here; treating that
     * as "date unknown" is a source-specific business rule and lives in the mapper. */
    fun parseToEpochMillis(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
            .getOrNull()
    }
}

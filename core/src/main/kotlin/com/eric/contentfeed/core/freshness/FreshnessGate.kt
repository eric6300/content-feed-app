package com.eric.contentfeed.core.freshness

import kotlin.time.Duration

fun interface EpochClock {
    fun nowEpochMillis(): Long
}

interface FreshnessGate {
    suspend fun isStale(
        key: String,
        ttl: Duration,
    ): Boolean

    suspend fun markFetched(key: String)
}

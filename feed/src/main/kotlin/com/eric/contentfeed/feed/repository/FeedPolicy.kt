package com.eric.contentfeed.feed.repository

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** Freshness/pagination/retention constants for the feed, per `USE_CASES.md` →
 * Feature: Feed freshness / Feature: Feed browsing. Kept as one small object rather
 * than scattering literals across the repositories that use them. */
internal object FeedPolicy {
    const val WEATHER_FRESHNESS_KEY = "weather"
    const val ARTICLES_FRESHNESS_KEY = "articles"

    val WEATHER_TTL: Duration = 15.minutes
    val ARTICLES_TTL: Duration = 1.hours

    const val ARTICLE_PAGE_SIZE = 20
    const val SERVICE_CARD_INTERVAL = 5

    val UNSAVED_ARTICLE_RETENTION: Duration = 7.days
}

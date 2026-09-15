package com.eric.contentfeed.feed.domain.model

/** A source-scoped status, distinct enough on its own that "loaded successfully with
 * zero items" ([Ready] + an empty list) and "failed with nothing cached" ([Failed] +
 * an empty list) never need a separate boolean flag to tell apart. */
sealed interface SourceStatus {
    data object Idle : SourceStatus

    data object Loading : SourceStatus

    data object Ready : SourceStatus

    data class Failed(
        val cause: RemoteFailure,
    ) : SourceStatus
}

/** The article stream's status is two-dimensional: [refresh] covers the top-of-stream
 * freshness check, [append] covers the independent next-page load triggered by
 * scrolling. A failed [append] must not disturb [refresh] or the items already on
 * screen (`USE_CASES.md` → "Loading the next page of articles fails"). */
data class ArticleStreamStatus(
    val refresh: SourceStatus,
    val append: SourceStatus,
    val isLastPage: Boolean,
)

/**
 * The full composed view of the feed, read from Room and assembled by
 * [com.eric.contentfeed.feed.domain.composition.FeedComposer]. [items] already
 * interleaves articles and service cards in render order; weather is surfaced
 * separately since it renders once, above the list, never as a [FeedItem].
 */
data class FeedSnapshot(
    val weather: WeatherData?,
    val weatherStatus: SourceStatus,
    val items: List<FeedItem>,
    val articleStreamStatus: ArticleStreamStatus,
)

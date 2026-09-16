package com.eric.contentfeed.feed.domain.model

/** Presentation-safe outcome for one live feed source. */
sealed interface SourceRefreshResult {
    data object Skipped : SourceRefreshResult

    data object Succeeded : SourceRefreshResult

    data class Failed(
        val cause: RemoteFailure,
    ) : SourceRefreshResult
}

data class FeedRefreshResult(
    val articles: SourceRefreshResult,
    val weather: SourceRefreshResult,
)

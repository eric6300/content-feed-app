package com.eric.contentfeed.feed.domain.composition

import com.eric.contentfeed.feed.domain.model.Article

/**
 * The article stream's canonical sort order — `published_at DESC, id ASC` — matching
 * `ArticleDao.observeArticles()`. SQLite's default for `DESC` is NULLS LAST, so a null
 * `publishedAtEpochMillis` (the "date unknown" fallback) sorts after every non-null
 * one. Shared by [ServiceCardPlacementAssigner] and [FeedComposer] so a placement's
 * anchor sort-key snapshot means exactly the same thing — "how many articles sort at
 * or above this point" — in both places.
 */
internal fun isAtOrAboveAnchor(
    publishedAtEpochMillis: Long?,
    id: Int,
    anchorPublishedAtEpochMillis: Long?,
    anchorId: Int,
): Boolean =
    when {
        publishedAtEpochMillis == null && anchorPublishedAtEpochMillis == null -> id <= anchorId
        publishedAtEpochMillis == null -> false
        anchorPublishedAtEpochMillis == null -> true
        publishedAtEpochMillis > anchorPublishedAtEpochMillis -> true
        publishedAtEpochMillis == anchorPublishedAtEpochMillis -> id <= anchorId
        else -> false
    }

/**
 * How many of [articles] sort at or above a placement anchored at
 * ([anchorPublishedAtEpochMillis], [anchorId]) — equivalently, the 0-indexed position
 * right after which that placement renders. Well-defined even when the anchor article
 * itself no longer exists in [articles]: it only compares the snapshot sort key
 * against whatever currently survives, which is exactly how a placement stays put
 * relative to its remaining neighbors after its anchor is pruned.
 */
internal fun countArticlesAtOrAboveAnchor(
    articles: List<Article>,
    anchorPublishedAtEpochMillis: Long?,
    anchorId: Int,
): Int =
    articles.count { article ->
        isAtOrAboveAnchor(article.publishedAtEpochMillis, article.id, anchorPublishedAtEpochMillis, anchorId)
    }

private fun isStrictlyAboveAnchor(
    publishedAtEpochMillis: Long?,
    id: Int,
    anchorPublishedAtEpochMillis: Long?,
    anchorId: Int,
): Boolean =
    when {
        publishedAtEpochMillis == null && anchorPublishedAtEpochMillis == null -> id < anchorId
        publishedAtEpochMillis == null -> false
        anchorPublishedAtEpochMillis == null -> true
        publishedAtEpochMillis > anchorPublishedAtEpochMillis -> true
        publishedAtEpochMillis == anchorPublishedAtEpochMillis -> id < anchorId
        else -> false
    }

/**
 * Like [countArticlesAtOrAboveAnchor], but excludes the anchor article itself when it
 * still survives. [ServiceCardPlacementAssigner] uses this — not the inclusive count —
 * to size its head window: sizing the window with the inclusive count would let a new
 * anchor land exactly on top of an existing placement's own (surviving) anchor
 * whenever the inclusive count happens to be a multiple of the interval, silently
 * dropped by `insertIfAbsent`'s conflict-ignore but wasting a pool-index slot for
 * nothing. Excluding the anchor sidesteps the collision outright.
 */
internal fun countArticlesStrictlyAboveAnchor(
    articles: List<Article>,
    anchorPublishedAtEpochMillis: Long?,
    anchorId: Int,
): Int =
    articles.count { article ->
        isStrictlyAboveAnchor(article.publishedAtEpochMillis, article.id, anchorPublishedAtEpochMillis, anchorId)
    }

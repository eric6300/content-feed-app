package com.eric.contentfeed.feed.domain.model

data class Article(
    val id: Int,
    val title: String,
    val source: String,
    val authors: List<String>,
    val summary: String?,
    val imageUrl: String?,
    val articleUrl: String,
    val publishedAtEpochMillis: Long?,
)

/**
 * [isSaved] reflects the domain-visible saved state, not the raw persisted flag: an
 * article pending removal from the Saved list (undo window still open) reports false
 * here so the feed/detail save indicator stays in sync with "no longer in the Saved
 * list" immediately, even though its data and local image are retained until the
 * window lapses.
 */
data class CachedArticle(
    val article: Article,
    val isSaved: Boolean,
    val savedAtEpochMillis: Long?,
    val localImagePath: String?,
    val pendingUnsaveAtEpochMillis: Long?,
)

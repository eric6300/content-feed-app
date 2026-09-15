package com.eric.contentfeed.feed.domain.model

/** One row in the composed, single scrollable feed list — the weather hero is not a
 * [FeedItem]; it is carried separately on [FeedSnapshot] since it appears once, above
 * the list, per `USE_CASES.md` → Feature: Feed browsing. */
sealed interface FeedItem {
    data class ArticleItem(
        val article: CachedArticle,
    ) : FeedItem

    /** [placement] is carried alongside the resolved [card] so callers have a stable,
     * anchor-based key for list diffing that survives the card's pool index cycling. */
    data class ServiceCardItem(
        val card: ServiceCard,
        val placement: FeedPlacement,
    ) : FeedItem
}

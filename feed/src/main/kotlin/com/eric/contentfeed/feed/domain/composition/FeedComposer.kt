package com.eric.contentfeed.feed.domain.composition

import com.eric.contentfeed.feed.data.local.ServiceCardCatalog
import com.eric.contentfeed.feed.domain.model.CachedArticle
import com.eric.contentfeed.feed.domain.model.FeedItem
import com.eric.contentfeed.feed.domain.model.FeedPlacement

/**
 * Merges the sorted article stream with assigned placements into one render-ordered
 * list. Weather is not composed here — it renders once, above this list (see
 * [com.eric.contentfeed.feed.domain.model.FeedSnapshot]).
 *
 * A pure function: no Room, no coroutines, no clock. [articles] must already be
 * sorted `published_at DESC, id ASC` (matching `ArticleDao.observeArticles()`), which
 * is the order Room already returns.
 */
object FeedComposer {
    fun compose(
        articles: List<CachedArticle>,
        placements: List<FeedPlacement>,
        serviceCardCatalog: ServiceCardCatalog,
    ): List<FeedItem> {
        if (placements.isEmpty()) return articles.map(FeedItem::ArticleItem)

        val sortKeys = articles.map { it.article }
        val placementsByInsertIndex =
            placements
                .groupBy { countArticlesAtOrAboveAnchor(sortKeys, it.anchorPublishedAtEpochMillis, it.anchorArticleId) }
                .mapValues { (_, atIndex) -> atIndex.sortedBy { it.assignmentSequence } }

        val result = ArrayList<FeedItem>(articles.size + placements.size)
        for (insertIndex in 0..articles.size) {
            placementsByInsertIndex[insertIndex]?.forEach { placement ->
                val card = serviceCardCatalog.cardForSlot(placement.poolIndex)
                // A pool that failed to parse any usable card at all (see
                // ServiceCardCatalog) drops the slot rather than rendering nothing
                // usable — the placement stays recorded and resolves once the pool is
                // fixed, since it is never re-derived once assigned.
                if (card != null) result += FeedItem.ServiceCardItem(card, placement)
            }
            if (insertIndex < articles.size) result += FeedItem.ArticleItem(articles[insertIndex])
        }
        return result
    }
}

package com.eric.contentfeed.feed.domain.composition

import com.eric.contentfeed.feed.data.local.FixedServiceCardCatalog
import com.eric.contentfeed.feed.domain.model.Article
import com.eric.contentfeed.feed.domain.model.CachedArticle
import com.eric.contentfeed.feed.domain.model.FeedItem
import com.eric.contentfeed.feed.domain.model.FeedPlacement
import com.eric.contentfeed.feed.domain.model.SERVICE_CARD_CONTENT_TYPE
import com.eric.contentfeed.feed.domain.model.ServiceCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeedComposerTest {
    @Test
    fun articlesRenderInGivenOrderWhenThereAreNoPlacements() {
        val articles = cachedArticles(1..5)

        val result = FeedComposer.compose(articles, emptyList(), FixedServiceCardCatalog(listOf(serviceCard(1))))

        assertEquals(articles.map(FeedItem::ArticleItem), result)
    }

    @Test
    fun cardRendersRightAfterItsAnchorArticle() {
        val articles = cachedArticles(1..10)
        val placement = placement(anchorId = 5, poolIndex = 0, sequence = 0)
        val card = serviceCard(1)

        val result = FeedComposer.compose(articles, listOf(placement), FixedServiceCardCatalog(listOf(card)))

        assertEquals(
            listOf(1, 2, 3, 4, 5, -1, 6, 7, 8, 9, 10),
            result.map { item ->
                if (item is FeedItem.ServiceCardItem) -1 else (item as FeedItem.ArticleItem).article.article.id
            },
        )
        assertEquals(FeedItem.ServiceCardItem(card, placement), result[5])
    }

    @Test
    fun multiplePlacementsInterleaveAtTheirOwnAnchors() {
        val articles = cachedArticles(1..12)
        val placements =
            listOf(
                placement(anchorId = 5, poolIndex = 0, sequence = 0),
                placement(anchorId = 10, poolIndex = 1, sequence = 1),
            )
        val catalog = FixedServiceCardCatalog(listOf(serviceCard(1)))

        val result = FeedComposer.compose(articles, placements, catalog)

        val cardPositions = result.withIndex().filter { it.value is FeedItem.ServiceCardItem }.map { it.index }
        assertEquals(listOf(5, 11), cardPositions)
    }

    @Test
    fun cardSurvivesItsAnchorArticleBeingPruned() {
        // Article 5 (the placement's anchor) is no longer in the cache.
        val survivors = cachedArticles(1..4) + cachedArticles(6..10)
        val placement = placement(anchorId = 5, poolIndex = 0, sequence = 0)
        val card = serviceCard(1)

        val result = FeedComposer.compose(survivors, listOf(placement), FixedServiceCardCatalog(listOf(card)))

        // Still renders right after the 4 remaining articles that were originally
        // above it — same relative position among the articles that remain.
        assertEquals(FeedItem.ServiceCardItem(card, placement), result[4])
        assertEquals(10, result.size)
    }

    @Test
    fun cardForSlotReturningNullDropsTheSlotInsteadOfCrashing() {
        val articles = cachedArticles(1..10)
        val placement = placement(anchorId = 5, poolIndex = 0, sequence = 0)

        val result = FeedComposer.compose(articles, listOf(placement), FixedServiceCardCatalog(emptyList()))

        assertEquals(articles.map(FeedItem::ArticleItem), result)
        assertNull(result.filterIsInstance<FeedItem.ServiceCardItem>().firstOrNull())
    }

    @Test
    fun poolIndexCyclesPastThePoolSizeViaTheCatalog() {
        val cards = listOf(serviceCard(1), serviceCard(2), serviceCard(3))
        val placement = placement(anchorId = 5, poolIndex = 7, sequence = 0)

        val result = FeedComposer.compose(cachedArticles(1..10), listOf(placement), FixedServiceCardCatalog(cards))

        val rendered = result.filterIsInstance<FeedItem.ServiceCardItem>().single()
        assertEquals(cards[7 % cards.size], rendered.card)
    }

    private fun cachedArticles(ids: IntRange): List<CachedArticle> = ids.map(::cachedArticle)

    private fun cachedArticle(id: Int) =
        CachedArticle(
            article =
                Article(
                    id = id,
                    title = "Article $id",
                    source = "Source",
                    authors = emptyList(),
                    summary = null,
                    imageUrl = null,
                    articleUrl = "https://example.com/$id",
                    publishedAtEpochMillis = -id.toLong(),
                ),
            isSaved = false,
            savedAtEpochMillis = null,
            localImagePath = null,
            pendingUnsaveAtEpochMillis = null,
        )

    private fun serviceCard(id: Int) =
        ServiceCard(
            id = id,
            title = "Card $id",
            description = "Description $id",
            blurb = "Blurb $id",
            price = 9.99,
            imageAssetPath = "service_cards/$id.png",
            targetUrl = "https://example.com/card/$id",
        )

    private fun placement(
        anchorId: Int,
        poolIndex: Int,
        sequence: Long,
    ) = FeedPlacement(
        anchorArticleId = anchorId,
        anchorPublishedAtEpochMillis = -anchorId.toLong(),
        contentType = SERVICE_CARD_CONTENT_TYPE,
        poolIndex = poolIndex,
        assignmentSequence = sequence,
    )
}

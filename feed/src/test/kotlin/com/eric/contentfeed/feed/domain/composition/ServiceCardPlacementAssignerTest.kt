package com.eric.contentfeed.feed.domain.composition

import com.eric.contentfeed.feed.domain.model.Article
import com.eric.contentfeed.feed.domain.model.FeedPlacement
import com.eric.contentfeed.feed.domain.model.SERVICE_CARD_CONTENT_TYPE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val INTERVAL = 5

class ServiceCardPlacementAssignerTest {
    @Test
    fun cardInsertedAfterEvery5thArticleWithNoExistingPlacements() {
        val result =
            ServiceCardPlacementAssigner.assign(
                articles = articles(1..12),
                existingPlacements = emptyList(),
                contentType = SERVICE_CARD_CONTENT_TYPE,
                interval = INTERVAL,
                nextPoolIndex = 0,
                nextAssignmentSequence = 0L,
            )

        assertEquals(listOf(5, 10), result.map { it.anchorArticleId })
        assertEquals(listOf(0, 1), result.map { it.poolIndex })
        assertEquals(listOf(0L, 1L), result.map { it.assignmentSequence })
    }

    @Test
    fun assignmentIsCumulativeAcrossAnAppendedPage() {
        val firstRound =
            ServiceCardPlacementAssigner.assign(
                articles = articles(1..12),
                existingPlacements = emptyList(),
                contentType = SERVICE_CARD_CONTENT_TYPE,
                interval = INTERVAL,
                nextPoolIndex = 0,
                nextAssignmentSequence = 0L,
            )

        val appended =
            ServiceCardPlacementAssigner.assign(
                articles = articles(1..20),
                existingPlacements = firstRound,
                contentType = SERVICE_CARD_CONTENT_TYPE,
                interval = INTERVAL,
                nextPoolIndex = 2,
                nextAssignmentSequence = 2L,
            )

        // The running count is not reset to zero for the new page — it continues from
        // where the bottom-most existing placement (anchored at 10) left off.
        assertEquals(listOf(15, 20), appended.map { it.anchorArticleId })
        assertEquals(listOf(2, 3), appended.map { it.poolIndex })
    }

    @Test
    fun prependPlacesWithinTheHeadWindowAndDiscardsTheLeftoverWithoutMovingExistingPlacements() {
        val base = articles(101..112)
        val existing =
            ServiceCardPlacementAssigner.assign(
                articles = base,
                existingPlacements = emptyList(),
                contentType = SERVICE_CARD_CONTENT_TYPE,
                interval = INTERVAL,
                nextPoolIndex = 0,
                nextAssignmentSequence = 0L,
            )
        assertEquals(listOf(105, 110), existing.map { it.anchorArticleId })

        // 7 new articles prepended above the entire existing stream.
        val prepended = articles(1..7) + base

        val result =
            ServiceCardPlacementAssigner.assign(
                articles = prepended,
                existingPlacements = existing,
                contentType = SERVICE_CARD_CONTENT_TYPE,
                interval = INTERVAL,
                nextPoolIndex = 2,
                nextAssignmentSequence = 2L,
            )

        // Head window = 11 articles strictly above anchor 105 (7 new + 101..104);
        // floor(11/5)=2 groups -> anchors at the window's 5th and 10th articles (5,
        // 103). The 2 leftover articles (104, and 105 itself) are not counted here —
        // 105 already has a placement, and the true leftover carries into next time.
        // Bottom window (below 110, now 17 articles at/above it) has only 2 articles
        // below — not enough for a new tail placement.
        assertEquals(listOf(5, 103), result.map { it.anchorArticleId })

        // The prepend's own count starts fresh from zero — it does not continue the
        // cumulative count that already placed cards further down the list, and
        // neither existing placement (105, 110) is renumbered or moved.
        assertTrue(result.none { it.anchorArticleId == 105 || it.anchorArticleId == 110 })
    }

    @Test
    fun interiorSegmentsBetweenExistingPlacementsAreNeverTouched() {
        val existing =
            listOf(
                placement(anchorId = 5, poolIndex = 0, sequence = 0),
                placement(anchorId = 10, poolIndex = 1, sequence = 1),
                placement(anchorId = 15, poolIndex = 2, sequence = 2),
            )

        val result =
            ServiceCardPlacementAssigner.assign(
                articles = articles(1..30),
                existingPlacements = existing,
                contentType = SERVICE_CARD_CONTENT_TYPE,
                interval = INTERVAL,
                nextPoolIndex = 3,
                nextAssignmentSequence = 3L,
            )

        // Only the tail below 15 grows; the two 5-article gaps between the existing
        // placements (5-10, 10-15) are settled and produce nothing, even though each
        // gap happens to be exactly one interval wide.
        assertEquals(listOf(20, 25, 30), result.map { it.anchorArticleId })
    }

    @Test
    fun reassigningWithNoNewArticlesProducesNoNewPlacements() {
        val firstRound =
            ServiceCardPlacementAssigner.assign(
                articles = articles(1..20),
                existingPlacements = emptyList(),
                contentType = SERVICE_CARD_CONTENT_TYPE,
                interval = INTERVAL,
                nextPoolIndex = 0,
                nextAssignmentSequence = 0L,
            )
        assertEquals(listOf(5, 10, 15, 20), firstRound.map { it.anchorArticleId })

        val secondRound =
            ServiceCardPlacementAssigner.assign(
                articles = articles(1..20),
                existingPlacements = firstRound,
                contentType = SERVICE_CARD_CONTENT_TYPE,
                interval = INTERVAL,
                nextPoolIndex = 4,
                nextAssignmentSequence = 4L,
            )

        assertEquals(emptyList<FeedPlacement>(), secondRound)
    }

    @Test
    fun assignmentContinuesCorrectlyAfterItsOwnAnchorArticleWasPruned() {
        // Article 5 (the sole existing placement's anchor) has since been pruned.
        val survivors = articles(1..4) + articles(6..12)
        val existing = listOf(placement(anchorId = 5, poolIndex = 0, sequence = 0))

        val result =
            ServiceCardPlacementAssigner.assign(
                articles = survivors,
                existingPlacements = existing,
                contentType = SERVICE_CARD_CONTENT_TYPE,
                interval = INTERVAL,
                nextPoolIndex = 1,
                nextAssignmentSequence = 1L,
            )

        // Only 4 articles now survive at/above the pruned anchor's snapshot position,
        // so the tail continues counting from 4, not from a stale "5" that assumed
        // article 5 still existed: the 5th surviving article below that point (id 10)
        // gets the next placement.
        assertEquals(listOf(10), result.map { it.anchorArticleId })
    }

    private fun articles(ids: IntRange): List<Article> = ids.map(::article)

    private fun article(id: Int) =
        Article(
            id = id,
            title = "Article $id",
            source = "Source",
            authors = emptyList(),
            summary = null,
            imageUrl = null,
            articleUrl = "https://example.com/$id",
            publishedAtEpochMillis = -id.toLong(),
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

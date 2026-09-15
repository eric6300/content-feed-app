package com.eric.contentfeed.feed.domain.composition

import com.eric.contentfeed.feed.domain.model.Article
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the null-timestamp branches of the anchor comparison shared by
 * [ServiceCardPlacementAssigner] and [FeedComposer] — the branches most likely to
 * silently flip sign if [isAtOrAboveAnchor]/[countArticlesStrictlyAboveAnchor]'s shared
 * comparator is ever touched again, since neither
 * `ServiceCardPlacementAssignerTest` nor `FeedComposerTest` exercises a null
 * `publishedAtEpochMillis` on the comparison path.
 */
class ArticleSortOrderTest {
    @Test
    fun articleWithNullTimestampSortsBelowAnAnchorWithANonNullTimestamp() {
        assertEquals(
            false,
            isAtOrAboveAnchor(
                publishedAtEpochMillis = null,
                id = 1,
                anchorPublishedAtEpochMillis = 100L,
                anchorId = 99,
            ),
        )
    }

    @Test
    fun articleWithNonNullTimestampSortsAboveAnAnchorWithANullTimestamp() {
        assertEquals(
            true,
            isAtOrAboveAnchor(
                publishedAtEpochMillis = 100L,
                id = 1,
                anchorPublishedAtEpochMillis = null,
                anchorId = 99,
            ),
        )
    }

    @Test
    fun bothTimestampsNullBreaksTheTieByArticleId() {
        assertEquals(
            true,
            isAtOrAboveAnchor(
                publishedAtEpochMillis = null,
                id = 5,
                anchorPublishedAtEpochMillis = null,
                anchorId = 10,
            ),
        )
        assertEquals(
            false,
            isAtOrAboveAnchor(
                publishedAtEpochMillis = null,
                id = 15,
                anchorPublishedAtEpochMillis = null,
                anchorId = 10,
            ),
        )
    }

    @Test
    fun countArticlesAtOrAboveAnchorTreatsANullAnchorTimestampAsSortingLast() {
        // Anchor has a null timestamp, so only the null-timestamp articles at/below
        // its own id count as "at or above" it — every dated article sorts above.
        val articles =
            listOf(
                article(id = 1, publishedAtEpochMillis = 500L),
                article(id = 2, publishedAtEpochMillis = null),
                article(id = 3, publishedAtEpochMillis = null),
            )

        val count =
            countArticlesAtOrAboveAnchor(
                articles,
                anchorPublishedAtEpochMillis = null,
                anchorId = 2,
            )

        assertEquals(2, count)
    }

    @Test
    fun countArticlesStrictlyAboveAnchorExcludesANullTimestampAnchorItself() {
        val articles =
            listOf(
                article(id = 1, publishedAtEpochMillis = 500L),
                article(id = 2, publishedAtEpochMillis = null),
            )

        val count =
            countArticlesStrictlyAboveAnchor(
                articles,
                anchorPublishedAtEpochMillis = null,
                anchorId = 2,
            )

        assertEquals(1, count)
    }

    private fun article(
        id: Int,
        publishedAtEpochMillis: Long?,
    ) = Article(
        id = id,
        title = "Article $id",
        source = "Source",
        authors = emptyList(),
        summary = null,
        imageUrl = null,
        articleUrl = "https://example.com/$id",
        publishedAtEpochMillis = publishedAtEpochMillis,
    )
}

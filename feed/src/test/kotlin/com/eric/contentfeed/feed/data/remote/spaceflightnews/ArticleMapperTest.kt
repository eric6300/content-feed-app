package com.eric.contentfeed.feed.data.remote.spaceflightnews

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArticleMapperTest {
    @Test
    fun fullyPopulatedArticleMapsEndToEnd() {
        val dto =
            article(
                imageUrl = "https://example.com/image.jpg",
                summary = "A real summary.",
                authors = listOf(AuthorDto("Ada Lovelace"), AuthorDto("Grace Hopper")),
                publishedAt = "2025-09-14T17:09:21Z",
            )

        val article = dto.toArticle()

        assertEquals(dto.id, article.id)
        assertEquals(dto.title, article.title)
        assertEquals(dto.newsSite, article.source)
        assertEquals(dto.url, article.articleUrl)
        assertEquals(listOf("Ada Lovelace", "Grace Hopper"), article.authors)
        assertEquals("A real summary.", article.summary)
        assertEquals("https://example.com/image.jpg", article.imageUrl)
        assertEquals(1_757_869_761_000L, article.publishedAtEpochMillis)
    }

    @Test
    fun emptyImageUrlBecomesNull() {
        assertNull(article(imageUrl = "").toArticle().imageUrl)
    }

    @Test
    fun absentImageUrlBecomesNull() {
        assertNull(article(imageUrl = null).toArticle().imageUrl)
    }

    @Test
    fun emptySummaryBecomesNull() {
        assertNull(article(summary = "").toArticle().summary)
    }

    @Test
    fun summaryWithLeadingWhitespaceIsTrimmed() {
        assertEquals("Trimmed.", article(summary = "\nTrimmed.").toArticle().summary)
    }

    @Test
    fun absentSummaryBecomesNull() {
        assertNull(article(summary = null).toArticle().summary)
    }

    @Test
    fun emptyAuthorsBecomesEmptyList() {
        assertEquals(emptyList<String>(), article(authors = emptyList()).toArticle().authors)
    }

    @Test
    fun absentAuthorsBecomesEmptyList() {
        assertEquals(emptyList<String>(), article(authors = null).toArticle().authors)
    }

    @Test
    fun authorWithNullNameIsDropped() {
        val authors = listOf(AuthorDto(name = "Real Name"), AuthorDto(name = null))
        assertEquals(listOf("Real Name"), article(authors = authors).toArticle().authors)
    }

    @Test
    fun epochSentinelPublishedAtBecomesNull() {
        assertNull(article(publishedAt = "1970-01-01T00:00:00Z").toArticle().publishedAtEpochMillis)
    }

    @Test
    fun aRealPublishedAtMapsToCorrectMillis() {
        assertEquals(
            1_757_869_761_000L,
            article(publishedAt = "2025-09-14T17:09:21Z").toArticle().publishedAtEpochMillis,
        )
    }

    @Test
    fun absentPublishedAtBecomesNull() {
        assertNull(article(publishedAt = null).toArticle().publishedAtEpochMillis)
    }

    @Test
    fun unparseablePublishedAtBecomesNull() {
        assertNull(article(publishedAt = "not-a-date").toArticle().publishedAtEpochMillis)
    }

    private fun article(
        id: Int = 1,
        title: String = "Title",
        url: String = "https://example.com/article",
        newsSite: String = "Source",
        imageUrl: String? = "https://example.com/image.jpg",
        summary: String? = "Summary",
        publishedAt: String? = "2025-09-14T17:09:21Z",
        authors: List<AuthorDto>? = emptyList(),
    ) = ArticleDto(
        id = id,
        title = title,
        url = url,
        newsSite = newsSite,
        imageUrl = imageUrl,
        summary = summary,
        publishedAt = publishedAt,
        authors = authors,
    )
}

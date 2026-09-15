package com.eric.contentfeed.feed.data.remote.spaceflightnews

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SpaceflightNewsDtoTest {
    private lateinit var moshi: Moshi

    @Before
    fun setUp() {
        moshi = Moshi.Builder().build()
    }

    @Test
    fun parsesARealTwoArticlePageResponse() {
        // Captured from a live call to
        // https://api.spaceflightnewsapi.net/v4/articles/?limit=2&offset=0
        val json =
            """
            {
              "count": 36045,
              "next": "https://api.spaceflightnewsapi.net/v4/articles/?limit=2&offset=2",
              "previous": null,
              "results": [
                {
                  "id": 39932,
                  "title": "NASA sets new date for Starliner return",
                  "authors": [
                    {"name": "SpaceNews Editor", "socials": null}
                  ],
                  "url": "https://spacenews.com/nasa-sets-new-date-for-starliner-return/",
                  "image_url": "https://spacenews.com/wp-content/uploads/2026/09/starliner.jpg",
                  "news_site": "SpaceNews",
                  "summary": "\nJoin us for a look at the latest Starliner developments.",
                  "published_at": "2026-09-14T17:09:21Z",
                  "updated_at": "2026-09-14T17:10:34.811453Z",
                  "featured": false,
                  "launches": [],
                  "events": []
                },
                {
                  "id": 39931,
                  "title": "Second article title",
                  "authors": [],
                  "url": "https://example.com/second-article/",
                  "image_url": "",
                  "news_site": "NASA Spaceflight",
                  "summary": "",
                  "published_at": "1970-01-01T00:00:00Z",
                  "updated_at": "2026-09-14T16:00:00Z",
                  "featured": false,
                  "launches": [],
                  "events": []
                }
              ]
            }
            """.trimIndent()

        val dto = moshi.adapter(ArticleListResponseDto::class.java).fromJson(json)!!

        assertTrue(dto.next!!.contains("offset=2"))
        assertEquals(2, dto.results.size)

        val first = dto.results[0]
        assertEquals(39932, first.id)
        assertEquals("NASA sets new date for Starliner return", first.title)
        assertEquals("https://spacenews.com/nasa-sets-new-date-for-starliner-return/", first.url)
        assertEquals("SpaceNews", first.newsSite)
        assertEquals("https://spacenews.com/wp-content/uploads/2026/09/starliner.jpg", first.imageUrl)
        assertEquals("2026-09-14T17:09:21Z", first.publishedAt)
        assertEquals(1, first.authors?.size)
        assertEquals("SpaceNews Editor", first.authors?.first()?.name)
        assertTrue(first.summary!!.startsWith("\n"))
    }

    @Test
    fun parsesTheEndOfPaginationResponse() {
        // Captured from offset past the total count.
        val json = """{"count":36045,"next":null,"previous":"...","results":[]}"""

        val dto = moshi.adapter(ArticleListResponseDto::class.java).fromJson(json)!!

        assertNull(dto.next)
        assertEquals(emptyList<ArticleDto>(), dto.results)
    }
}

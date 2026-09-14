package com.eric.contentfeed.feed.data.local

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ArticleAuthorsJsonCodecTest {
    private lateinit var codec: ArticleAuthorsJsonCodec

    @Before
    fun setUp() {
        codec = ArticleAuthorsJsonCodec(Moshi.Builder().build())
    }

    @Test
    fun roundTripPreservesAuthors() {
        val authors = listOf("Ada Lovelace", "Grace Hopper")

        assertEquals(authors, codec.decode(codec.encode(authors)))
    }

    @Test
    fun malformedJsonBecomesAnEmptyList() {
        assertEquals(emptyList<String>(), codec.decode("not-json"))
    }
}

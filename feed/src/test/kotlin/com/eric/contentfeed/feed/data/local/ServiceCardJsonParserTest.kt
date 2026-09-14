package com.eric.contentfeed.feed.data.local

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ServiceCardJsonParserTest {
    private lateinit var parser: ServiceCardJsonParser

    @Before
    fun setUp() {
        parser = ServiceCardJsonParser(Moshi.Builder().build())
    }

    @Test
    fun parsesEntriesInOrderAndSynthesizesATargetUrlFromTheTitle() {
        val json =
            """
            [
              {"id": 1, "title": "Orbit Light", "description": "d1", "blurb": "b1", "price": 12.0, "imageAssetPath": "a1.svg"},
              {"id": 2, "title": "Signal Notebook", "description": "d2", "blurb": "b2", "price": 8.0, "imageAssetPath": "a2.svg"}
            ]
            """.trimIndent()

        val cards = parser.parse(json)

        assertEquals(listOf(1, 2), cards.map { it.id })
        assertEquals(
            "https://www.google.com/search?q=Orbit+Light",
            cards.first().targetUrl,
        )
    }

    @Test
    fun aMalformedEntryIsSkippedWhileValidEntriesAreStillLoaded() {
        val json =
            """
            [
              {"id": 1, "title": "Orbit Light", "description": "d1", "blurb": "b1", "price": 12.0, "imageAssetPath": "a1.svg"},
              {"id": 2, "description": "d2", "blurb": "b2", "price": 8.0, "imageAssetPath": "a2.svg"},
              {"id": 3, "title": "Dispatch Tote", "description": "d3", "blurb": "b3", "price": 24.0, "imageAssetPath": "a3.svg"}
            ]
            """.trimIndent()

        val cards = parser.parse(json)

        assertEquals(listOf(1, 3), cards.map { it.id })
    }

    @Test
    fun malformedTopLevelJsonProducesAnEmptyPoolInsteadOfCrashing() {
        assertEquals(emptyList<Any>(), parser.parse("not-json"))
    }
}

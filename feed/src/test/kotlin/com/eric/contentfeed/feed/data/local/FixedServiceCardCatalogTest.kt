package com.eric.contentfeed.feed.data.local

import com.eric.contentfeed.feed.domain.model.ServiceCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FixedServiceCardCatalogTest {
    private val catalog =
        FixedServiceCardCatalog(
            listOf(
                ServiceCard(1, "First", "", "", null, "", ""),
                ServiceCard(2, "Second", "", "", null, "", ""),
            ),
        )

    @Test
    fun slotsCycleThroughTheConfiguredCards() {
        assertEquals(1, catalog.cardForSlot(0).id)
        assertEquals(2, catalog.cardForSlot(1).id)
        assertEquals(1, catalog.cardForSlot(2).id)
    }

    @Test
    fun negativeSlotIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { catalog.cardForSlot(-1) }
    }
}

package com.eric.contentfeed.feed.data.local

import android.content.Context
import com.eric.contentfeed.feed.domain.model.ServiceCard
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.net.URLEncoder

@JsonClass(generateAdapter = true)
internal data class ServiceCardAssetJson(
    val id: Int,
    val title: String,
    val description: String,
    val blurb: String,
    val price: Double?,
    val imageAssetPath: String,
)

interface ServiceCardCatalog {
    fun cardForSlot(slotIndex: Int): ServiceCard
}

class FixedServiceCardCatalog(
    private val cards: List<ServiceCard>,
) : ServiceCardCatalog {
    init {
        require(cards.isNotEmpty()) { "Service card catalog must not be empty" }
    }

    override fun cardForSlot(slotIndex: Int): ServiceCard {
        require(slotIndex >= 0) { "Service card slot must not be negative" }
        return cards[slotIndex % cards.size]
    }
}

/**
 * The live DummyJSON catalog this app's snapshot is seeded from has no products with
 * missing fields, so per-field fallbacks aren't attempted here. The one case worth
 * guarding is the pool file itself being malformed (a hand-edited local JSON file) — a
 * single bad entry is skipped rather than failing the whole pool.
 */
internal class ServiceCardJsonParser(
    private val moshi: Moshi,
) {
    private val rawListAdapter =
        moshi.adapter<List<Any?>>(Types.newParameterizedType(List::class.java, Any::class.java))
    private val entryAdapter = moshi.adapter(ServiceCardAssetJson::class.java)

    fun parse(json: String): List<ServiceCard> {
        val rawEntries = runCatching { rawListAdapter.fromJson(json) }.getOrNull().orEmpty()
        return rawEntries
            .mapNotNull { raw -> runCatching { entryAdapter.fromJsonValue(raw) }.getOrNull() }
            .map(::toServiceCard)
    }

    private fun toServiceCard(item: ServiceCardAssetJson) =
        ServiceCard(
            id = item.id,
            title = item.title,
            description = item.description,
            blurb = item.blurb,
            price = item.price,
            imageAssetPath = item.imageAssetPath,
            targetUrl = "https://www.google.com/search?q=${URLEncoder.encode(item.title, "UTF-8")}",
        )
}

class BundledServiceCardCatalog(
    private val context: Context,
    moshi: Moshi,
) : ServiceCardCatalog {
    private val parser = ServiceCardJsonParser(moshi)
    private val delegate by lazy { FixedServiceCardCatalog(loadCards()) }

    override fun cardForSlot(slotIndex: Int): ServiceCard = delegate.cardForSlot(slotIndex)

    private fun loadCards(): List<ServiceCard> {
        val json =
            context.assets
                .open("service_cards.json")
                .bufferedReader()
                .use { it.readText() }
        return parser.parse(json)
    }
}

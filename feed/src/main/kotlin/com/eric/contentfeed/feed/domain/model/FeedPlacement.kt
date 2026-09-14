package com.eric.contentfeed.feed.domain.model

/** Plain string key, not a shared enum — a future second insertable content type costs
 * no changes here, only its own constant and pool-index cycle. */
const val SERVICE_CARD_CONTENT_TYPE = "service_card"

data class FeedPlacement(
    val anchorArticleId: Int,
    val anchorPublishedAtEpochMillis: Long?,
    val contentType: String,
    val poolIndex: Int,
    val assignmentSequence: Long,
)

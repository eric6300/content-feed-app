package com.eric.contentfeed.core.database

import androidx.room.Entity

/**
 * A slot inserted into the article stream. Anchored by a snapshot of the neighboring
 * article's sort key, not a live foreign key — a placement must stay valid after that
 * article is later pruned from the cache (see DECISIONS.md: "Sticky service-card
 * placement anchors on a sort-key snapshot").
 *
 * [contentType] is a plain string key (e.g. "service_card"), not a shared enum, so a
 * future second insertable content type costs no changes to this file — only a new
 * caller-side constant and its own [poolIndex] cycle. [poolIndex] cycles within
 * [contentType]; [assignmentSequence] is the global insertion order across all types,
 * used to render placements in the order they were assigned.
 */
@Entity(
    tableName = "feed_placements",
    primaryKeys = ["anchorArticleId", "contentType"],
)
data class FeedPlacementEntity(
    val anchorArticleId: Int,
    val anchorPublishedAtEpochMillis: Long?,
    val contentType: String,
    val poolIndex: Int,
    val assignmentSequence: Long,
)

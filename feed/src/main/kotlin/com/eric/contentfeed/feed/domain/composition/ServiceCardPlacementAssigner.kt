package com.eric.contentfeed.feed.domain.composition

import com.eric.contentfeed.feed.domain.model.Article
import com.eric.contentfeed.feed.domain.model.FeedPlacement

/**
 * Computes which *new* sticky placements to insert for one content type, as a pure
 * function of the current article stream and the placements already assigned. No
 * running counter is persisted anywhere.
 *
 * **Load-bearing invariant:** the bottom-most existing placement for a content type
 * always sits exactly on a multiple-of-[interval] boundary of the article stream
 * below it (every placement was itself created by this same rule). Restarting the
 * count at that anchor is therefore identical to having carried a cumulative counter
 * across pages — nothing needs to be persisted beyond the placements themselves.
 *
 * Two regions can grow new placements on a call:
 * - **Tail** — articles below the bottom-most existing placement (or the whole stream,
 *   counted from zero, if none exist yet). This is the pagination/append path.
 * - **Head** — articles above the topmost existing placement, also counted from zero.
 *   This is the refresh/prepend path. The head window is *region*-based, not
 *   batch-based: any leftover articles above the topmost placement that didn't
 *   complete a full interval last time are recounted into this call's head window
 *   rather than tracked as a separate carry-over batch.
 * - Articles strictly between two existing placements are a settled interior segment
 *   and are never touched.
 *
 * Calling this again with an unchanged (articles, placements) pair always returns an
 * empty list: the first call's insertions become part of `existingPlacements` on the
 * next call, which shrinks both windows down to a sub-[interval] leftover.
 */
object ServiceCardPlacementAssigner {
    fun assign(
        articles: List<Article>,
        existingPlacements: List<FeedPlacement>,
        contentType: String,
        interval: Int,
        nextPoolIndex: Int,
        nextAssignmentSequence: Long,
    ): List<FeedPlacement> {
        require(interval > 0) { "interval must be positive" }
        if (articles.isEmpty()) return emptyList()

        val relevant = existingPlacements.filter { it.contentType == contentType }
        val anchors =
            if (relevant.isEmpty()) {
                anchorsInRegion(articles, regionStart = 0, regionEnd = articles.size, interval)
            } else {
                val positions =
                    relevant.map {
                        it to
                            countArticlesAtOrAboveAnchor(articles, it.anchorPublishedAtEpochMillis, it.anchorArticleId)
                    }
                val topMostPlacement = positions.minBy { it.second }.first
                val bottomMost = positions.maxOf { it.second }
                // Head boundary excludes the topmost anchor itself (see
                // countArticlesStrictlyAboveAnchor) so a new anchor can never land
                // exactly on an existing, still-surviving one. The tail boundary needs
                // no such adjustment: [bottomMost] (inclusive) already points at the
                // first not-yet-placed article, since appended content never changes
                // an existing anchor's own count.
                val headRegionSize =
                    countArticlesStrictlyAboveAnchor(
                        articles,
                        topMostPlacement.anchorPublishedAtEpochMillis,
                        topMostPlacement.anchorArticleId,
                    )
                anchorsInRegion(articles, regionStart = 0, regionEnd = headRegionSize, interval) +
                    anchorsInRegion(articles, regionStart = bottomMost, regionEnd = articles.size, interval)
            }

        return anchors.mapIndexed { index, anchor ->
            FeedPlacement(
                anchorArticleId = anchor.id,
                anchorPublishedAtEpochMillis = anchor.publishedAtEpochMillis,
                contentType = contentType,
                poolIndex = nextPoolIndex + index,
                assignmentSequence = nextAssignmentSequence + index,
            )
        }
    }

    /** Every [interval]-th article within `[regionStart, regionEnd)`, counted from
     * [regionStart], in top-to-bottom order. A partial final group short of a full
     * interval is left uncounted, per the class doc's region-based leftover rule. */
    private fun anchorsInRegion(
        articles: List<Article>,
        regionStart: Int,
        regionEnd: Int,
        interval: Int,
    ): List<Article> {
        val regionSize = regionEnd - regionStart
        if (regionSize <= 0) return emptyList()
        val newPlacementCount = regionSize / interval
        return (1..newPlacementCount).map { k -> articles[regionStart + k * interval - 1] }
    }
}

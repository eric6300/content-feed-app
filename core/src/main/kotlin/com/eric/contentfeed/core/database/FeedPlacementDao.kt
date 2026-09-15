package com.eric.contentfeed.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedPlacementDao {
    @Query(
        """
        SELECT * FROM feed_placements
        ORDER BY assignmentSequence ASC
        """,
    )
    fun observePlacements(): Flow<List<FeedPlacementEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(placement: FeedPlacementEntity)

    @Query(
        """
        SELECT * FROM feed_placements
        WHERE anchorArticleId = :articleId AND contentType = :contentType
        LIMIT 1
        """,
    )
    suspend fun findByAnchorArticleId(
        articleId: Int,
        contentType: String,
    ): FeedPlacementEntity?

    /** Next pool index to assign for [contentType]. `MAX(poolIndex) + 1`, not a row
     * count — a count breaks the moment [deleteOrphanedBelow] removes a row, since the
     * cycle must keep advancing from where it left off, not shrink back. */
    @Query(
        """
        SELECT COALESCE(MAX(poolIndex), -1) + 1 FROM feed_placements WHERE contentType = :contentType
        """,
    )
    suspend fun nextPoolIndex(contentType: String): Int

    /** Next global assignment sequence, across all content types. Same `MAX + 1`
     * reasoning as [nextPoolIndex]: a deleted row must not free up its sequence
     * number for reuse. */
    @Query("SELECT COALESCE(MAX(assignmentSequence), -1) + 1 FROM feed_placements")
    suspend fun nextAssignmentSequence(): Long

    /** Removes placements anchored below the oldest surviving article, i.e. orphaned
     * by the retention cleanup pruning a whole fetch session at once rather than one
     * article at a time. A placement anchored to a pruned article that still has
     * surviving neighbours below it is unaffected — its snapshot key still falls
     * above the cutoff, so it renders at the same relative position as before (see
     * DECISIONS.md: "Sticky service-card placement anchors on a sort-key snapshot").
     *
     * Mirrors [ArticleDao.observeArticles]'s `publishedAtEpochMillis DESC, id ASC`
     * comparator, where SQLite's default NULLS LAST for DESC means a null timestamp
     * sorts below every non-null one — hence the explicit CASE instead of a plain
     * `<`/`=` comparison, which would silently no-op on either side being null. */
    @Query(
        """
        DELETE FROM feed_placements
        WHERE CASE
            WHEN anchorPublishedAtEpochMillis IS NULL AND :oldestSurvivingPublishedAtEpochMillis IS NULL
                THEN anchorArticleId > :oldestSurvivingArticleId
            WHEN anchorPublishedAtEpochMillis IS NULL THEN 1
            WHEN :oldestSurvivingPublishedAtEpochMillis IS NULL THEN 0
            WHEN anchorPublishedAtEpochMillis < :oldestSurvivingPublishedAtEpochMillis THEN 1
            WHEN anchorPublishedAtEpochMillis = :oldestSurvivingPublishedAtEpochMillis
                THEN anchorArticleId > :oldestSurvivingArticleId
            ELSE 0
        END
        """,
    )
    suspend fun deleteOrphanedBelow(
        oldestSurvivingPublishedAtEpochMillis: Long?,
        oldestSurvivingArticleId: Int,
    ): Int

    /** Used when retention cleanup leaves zero surviving articles — nothing remains
     * for any placement to anchor to, so [deleteOrphanedBelow]'s "below the oldest
     * survivor" comparison has no meaningful survivor to compare against. */
    @Query("DELETE FROM feed_placements")
    suspend fun deleteAll(): Int
}

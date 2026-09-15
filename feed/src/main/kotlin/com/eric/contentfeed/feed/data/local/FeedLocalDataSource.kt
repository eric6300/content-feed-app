package com.eric.contentfeed.feed.data.local

import com.eric.contentfeed.feed.domain.model.Article
import com.eric.contentfeed.feed.domain.model.CachedArticle
import com.eric.contentfeed.feed.domain.model.FeedPlacement
import com.eric.contentfeed.feed.domain.model.WeatherData
import kotlinx.coroutines.flow.Flow

interface FeedLocalDataSource {
    fun observeArticles(): Flow<List<CachedArticle>>

    fun observeSavedArticles(): Flow<List<CachedArticle>>

    fun observeArticle(articleId: Int): Flow<CachedArticle?>

    fun observeWeather(): Flow<WeatherData?>

    fun observePlacements(): Flow<List<FeedPlacement>>

    /** Next pool index to assign within [contentType] — `MAX(poolIndex) + 1`, immune
     * to rows [deleteOrphanedPlacementsBelow] has removed. */
    suspend fun nextPlacementPoolIndex(contentType: String): Int

    /** Next global assignment sequence across all content types — same `MAX + 1`
     * reasoning as [nextPlacementPoolIndex]. */
    suspend fun nextPlacementAssignmentSequence(): Long

    /** Removes placement rows orphaned by the retention cleanup pruning a whole fetch
     * session of unsaved articles at once. Pass the oldest surviving article's sort
     * key (`published_at`, `id`), or `null`/any value when there are no surviving
     * articles to protect. Returns the number of rows removed. */
    suspend fun deleteOrphanedPlacementsBelow(
        oldestSurvivingPublishedAtEpochMillis: Long?,
        oldestSurvivingArticleId: Int,
    ): Int

    /** Used when retention cleanup leaves zero surviving articles — see
     * [deleteOrphanedPlacementsBelow]. */
    suspend fun deleteAllPlacements(): Int

    suspend fun upsertArticles(
        articles: List<Article>,
        fetchedAtEpochMillis: Long,
    )

    suspend fun upsertWeather(weather: WeatherData)

    suspend fun insertPlacement(placement: FeedPlacement)

    /** Returns false if [articleId] doesn't exist in the local cache yet, in which case
     * the save had no effect. */
    suspend fun saveArticle(
        articleId: Int,
        savedAtEpochMillis: Long,
        localImagePath: String?,
    ): Boolean

    /** Feed/detail toggle: immediate, full removal — no undo for this path. */
    suspend fun unsaveArticleImmediately(articleId: Int)

    /** Saved-list removal: hides the article from the Saved list but keeps its data
     * until [deadlineEpochMillis] passes, in case the user taps undo. Returns false if
     * the article wasn't currently saved. */
    suspend fun markPendingUnsave(
        articleId: Int,
        deadlineEpochMillis: Long,
    ): Boolean

    /** Returns false if there was nothing pending to undo as of [nowEpochMillis] —
     * already finalized, never removed, or its own deadline has already passed even if
     * [finalizeExpiredPendingUnsaves] hasn't run yet. */
    suspend fun undoUnsave(
        articleId: Int,
        nowEpochMillis: Long,
    ): Boolean

    /** Finalizes every pending removal whose undo window has passed as of
     * [nowEpochMillis], returning the removed articles so callers can delete their
     * locally-copied images. */
    suspend fun finalizeExpiredPendingUnsaves(nowEpochMillis: Long): List<CachedArticle>

    suspend fun pruneUnsavedArticles(cutoffEpochMillis: Long): Int
}

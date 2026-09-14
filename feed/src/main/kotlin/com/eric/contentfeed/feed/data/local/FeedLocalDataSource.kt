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

    suspend fun countPlacementsByContentType(contentType: String): Int

    suspend fun upsertArticles(
        articles: List<Article>,
        fetchedAtEpochMillis: Long,
    )

    suspend fun upsertWeather(weather: WeatherData)

    suspend fun insertPlacement(placement: FeedPlacement)

    suspend fun saveArticle(
        articleId: Int,
        savedAtEpochMillis: Long,
        localImagePath: String?,
    )

    /** Feed/detail toggle: immediate, full removal — no undo for this path. */
    suspend fun unsaveArticleImmediately(articleId: Int)

    /** Saved-list removal: hides the article from the Saved list but keeps its data
     * until [deadlineEpochMillis] passes, in case the user taps undo. Returns false if
     * the article wasn't currently saved. */
    suspend fun markPendingUnsave(
        articleId: Int,
        deadlineEpochMillis: Long,
    ): Boolean

    /** Returns false if there was nothing pending to undo (already finalized, or never
     * removed). */
    suspend fun undoUnsave(articleId: Int): Boolean

    /** Finalizes every pending removal whose undo window has passed as of
     * [nowEpochMillis], returning the removed articles so callers can delete their
     * locally-copied images. */
    suspend fun finalizeExpiredPendingUnsaves(nowEpochMillis: Long): List<CachedArticle>

    suspend fun pruneUnsavedArticles(cutoffEpochMillis: Long): Int
}

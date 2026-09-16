package com.eric.contentfeed.feed.repository

import com.eric.contentfeed.feed.domain.model.CachedArticle
import kotlinx.coroutines.flow.Flow

/** Local-only source of truth for saved article state and offline detail content. */
interface SavedArticleRepository {
    fun observeSavedArticles(): Flow<List<CachedArticle>>

    fun observeArticle(articleId: Int): Flow<CachedArticle?>

    suspend fun saveArticle(articleId: Int)

    suspend fun unsaveArticleImmediately(articleId: Int)

    suspend fun removeFromSavedList(articleId: Int): Boolean

    suspend fun undoRemoval(articleId: Int): Boolean

    /** Finalizes one Saved-list removal when its undo affordance is dismissed. */
    suspend fun finalizeRemoval(articleId: Int)

    /** Defensive sweep for pending rows whose persisted deadlines have elapsed. */
    suspend fun finalizeExpiredRemovals()

    suspend fun finalizeAllPendingRemovals()
}

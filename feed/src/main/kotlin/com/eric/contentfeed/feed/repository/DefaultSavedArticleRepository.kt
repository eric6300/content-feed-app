package com.eric.contentfeed.feed.repository

import com.eric.contentfeed.core.freshness.EpochClock
import com.eric.contentfeed.feed.data.local.FeedLocalDataSource
import com.eric.contentfeed.feed.data.local.SavedImageStore
import com.eric.contentfeed.feed.domain.model.CachedArticle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Coordinates local saved-state commands. Room remains authoritative for state; the
 * image store is only an injectable side effect for offline copies and cleanup. */
internal class DefaultSavedArticleRepository(
    private val localDataSource: FeedLocalDataSource,
    private val savedImageStore: SavedImageStore,
    private val clock: EpochClock,
) : SavedArticleRepository {
    private val imageMutationMutex = Mutex()

    override fun observeSavedArticles(): Flow<List<CachedArticle>> = localDataSource.observeSavedArticles()

    override fun observeArticle(articleId: Int): Flow<CachedArticle?> = localDataSource.observeArticle(articleId)

    override suspend fun saveArticle(articleId: Int) {
        val saved =
            localDataSource.saveArticle(
                articleId = articleId,
                savedAtEpochMillis = clock.nowEpochMillis(),
                localImagePath = null,
            )
        if (!saved) return

        imageMutationMutex.withLock {
            val imageUrl =
                localDataSource
                    .observeArticle(articleId)
                    .first()
                    ?.article
                    ?.imageUrl
            val copiedPath = savedImageStore.copyFromCache(articleId, imageUrl)
            if (copiedPath != null && !localDataSource.attachLocalImagePath(articleId, copiedPath)) {
                // An immediate unsave may have cleared the row while the copy was in
                // flight. Do not leave an unreferenced file behind.
                savedImageStore.delete(articleId)
            }
        }
    }

    override suspend fun unsaveArticleImmediately(articleId: Int) {
        localDataSource.unsaveArticleImmediately(articleId)
        imageMutationMutex.withLock { savedImageStore.delete(articleId) }
    }

    override suspend fun removeFromSavedList(articleId: Int): Boolean =
        localDataSource.markPendingUnsave(
            articleId = articleId,
            deadlineEpochMillis = clock.nowEpochMillis() + FeedPolicy.PERSISTED_UNDO_WINDOW.inWholeMilliseconds,
        )

    override suspend fun undoRemoval(articleId: Int): Boolean =
        localDataSource.undoUnsave(articleId, clock.nowEpochMillis())

    override suspend fun finalizeExpiredRemovals() {
        val finalized = localDataSource.finalizeExpiredPendingUnsaves(clock.nowEpochMillis())
        deleteImages(finalized)
    }

    override suspend fun finalizeAllPendingRemovals() {
        val finalized = localDataSource.finalizeAllPendingUnsaves()
        deleteImages(finalized)
    }

    private suspend fun deleteImages(articles: List<CachedArticle>) {
        if (articles.isEmpty()) return
        imageMutationMutex.withLock {
            articles.forEach { savedImageStore.delete(it.article.id) }
        }
    }
}

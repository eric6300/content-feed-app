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
    private val savedMutationMutex = Mutex()

    override fun observeSavedArticles(): Flow<List<CachedArticle>> = localDataSource.observeSavedArticles()

    override fun observeArticle(articleId: Int): Flow<CachedArticle?> = localDataSource.observeArticle(articleId)

    override suspend fun saveArticle(articleId: Int) {
        savedMutationMutex.withLock {
            val saved =
                localDataSource.saveArticle(
                    articleId = articleId,
                    savedAtEpochMillis = clock.nowEpochMillis(),
                    // The DAO preserves an existing local image when this is null.
                    localImagePath = null,
                )
            if (!saved) return@withLock

            val imageUrl =
                localDataSource
                    .observeArticle(articleId)
                    .first()
                    ?.article
                    ?.imageUrl
            val copiedPath = savedImageStore.copyFromCache(articleId, imageUrl)
            if (copiedPath != null && !localDataSource.attachLocalImagePath(articleId, copiedPath)) {
                // Keep the deterministic file store tidy if the guarded attach rejects
                // a stale copy (for example, after an external state change).
                savedImageStore.delete(articleId)
            }
        }
    }

    override suspend fun unsaveArticleImmediately(articleId: Int) {
        savedMutationMutex.withLock {
            localDataSource.unsaveArticleImmediately(articleId)
            savedImageStore.delete(articleId)
        }
    }

    override suspend fun removeFromSavedList(articleId: Int): Boolean =
        savedMutationMutex.withLock {
            localDataSource.markPendingUnsave(
                articleId = articleId,
                deadlineEpochMillis =
                    clock.nowEpochMillis() + FeedPolicy.PERSISTED_UNDO_WINDOW.inWholeMilliseconds,
            )
        }

    override suspend fun undoRemoval(articleId: Int): Boolean =
        savedMutationMutex.withLock {
            localDataSource.undoUnsave(articleId, clock.nowEpochMillis())
        }

    override suspend fun finalizeRemoval(articleId: Int) {
        savedMutationMutex.withLock {
            val finalized = localDataSource.finalizePendingUnsave(articleId)
            if (finalized != null) savedImageStore.delete(finalized.article.id)
        }
    }

    override suspend fun finalizeExpiredRemovals() {
        savedMutationMutex.withLock {
            val finalized = localDataSource.finalizeExpiredPendingUnsaves(clock.nowEpochMillis())
            deleteImagesLocked(finalized)
        }
    }

    override suspend fun finalizeAllPendingRemovals() {
        savedMutationMutex.withLock {
            val finalized = localDataSource.finalizeAllPendingUnsaves()
            deleteImagesLocked(finalized)
        }
    }

    private suspend fun deleteImagesLocked(articles: List<CachedArticle>) {
        articles.forEach { savedImageStore.delete(it.article.id) }
    }
}

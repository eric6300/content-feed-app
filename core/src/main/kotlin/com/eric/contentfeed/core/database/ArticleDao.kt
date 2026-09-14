package com.eric.contentfeed.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ArticleDao {
    @Query(
        """
        SELECT * FROM articles
        ORDER BY publishedAtEpochMillis DESC, id ASC
        """,
    )
    abstract fun observeArticles(): Flow<List<ArticleEntity>>

    @Query(
        """
        SELECT * FROM articles
        WHERE isSaved = 1 AND pendingUnsaveAtEpochMillis IS NULL
        ORDER BY savedAtEpochMillis DESC, id ASC
        """,
    )
    abstract fun observeSavedArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :articleId LIMIT 1")
    abstract fun observeArticle(articleId: Int): Flow<ArticleEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertIfMissing(article: ArticleEntity): Long

    @Query(
        """
        UPDATE articles
        SET title = :title,
            source = :source,
            authorsJson = :authorsJson,
            summary = :summary,
            imageUrl = :imageUrl,
            articleUrl = :articleUrl,
            publishedAtEpochMillis = :publishedAtEpochMillis,
            fetchedAtEpochMillis = :fetchedAtEpochMillis
        WHERE id = :id
        """,
    )
    protected abstract suspend fun updateRemoteContent(
        id: Int,
        title: String,
        source: String,
        authorsJson: String,
        summary: String?,
        imageUrl: String?,
        articleUrl: String,
        publishedAtEpochMillis: Long?,
        fetchedAtEpochMillis: Long,
    )

    /** Preserves save state and pending-unsave bookkeeping for rows that already exist. */
    @Transaction
    open suspend fun upsertRemoteArticles(articles: List<ArticleEntity>) {
        articles.forEach { article ->
            insertIfMissing(article)
            updateRemoteContent(
                id = article.id,
                title = article.title,
                source = article.source,
                authorsJson = article.authorsJson,
                summary = article.summary,
                imageUrl = article.imageUrl,
                articleUrl = article.articleUrl,
                publishedAtEpochMillis = article.publishedAtEpochMillis,
                fetchedAtEpochMillis = article.fetchedAtEpochMillis,
            )
        }
    }

    @Query(
        """
        UPDATE articles
        SET isSaved = 1,
            savedAtEpochMillis = :savedAtEpochMillis,
            localImagePath = :localImagePath,
            pendingUnsaveAtEpochMillis = NULL
        WHERE id = :articleId
        """,
    )
    abstract suspend fun saveArticle(
        articleId: Int,
        savedAtEpochMillis: Long,
        localImagePath: String?,
    ): Int

    /** Feed/detail toggle: immediate, full removal — there is no undo for this path. */
    @Query(
        """
        UPDATE articles
        SET isSaved = 0,
            savedAtEpochMillis = NULL,
            localImagePath = NULL,
            pendingUnsaveAtEpochMillis = NULL
        WHERE id = :articleId
        """,
    )
    abstract suspend fun unsaveImmediate(articleId: Int)

    /** Saved-list removal: keeps save state/local image intact until the undo window lapses. */
    @Query(
        """
        UPDATE articles
        SET pendingUnsaveAtEpochMillis = :deadlineEpochMillis
        WHERE id = :articleId AND isSaved = 1
        """,
    )
    abstract suspend fun markPendingUnsave(
        articleId: Int,
        deadlineEpochMillis: Long,
    ): Int

    /** No-ops (returns 0) once [nowEpochMillis] has reached or passed the pending
     * deadline, even if [finalizeExpiredPendingUnsaves] hasn't run yet — undo must not
     * succeed after its own window has closed. */
    @Query(
        """
        UPDATE articles
        SET pendingUnsaveAtEpochMillis = NULL
        WHERE id = :articleId
          AND pendingUnsaveAtEpochMillis IS NOT NULL
          AND pendingUnsaveAtEpochMillis > :nowEpochMillis
        """,
    )
    abstract suspend fun undoPendingUnsave(
        articleId: Int,
        nowEpochMillis: Long,
    ): Int

    @Query(
        """
        SELECT * FROM articles
        WHERE pendingUnsaveAtEpochMillis IS NOT NULL AND pendingUnsaveAtEpochMillis <= :nowEpochMillis
        """,
    )
    protected abstract suspend fun selectExpiredPendingUnsaves(nowEpochMillis: Long): List<ArticleEntity>

    @Query(
        """
        UPDATE articles
        SET isSaved = 0,
            savedAtEpochMillis = NULL,
            localImagePath = NULL,
            pendingUnsaveAtEpochMillis = NULL
        WHERE pendingUnsaveAtEpochMillis IS NOT NULL AND pendingUnsaveAtEpochMillis <= :nowEpochMillis
        """,
    )
    protected abstract suspend fun clearExpiredPendingUnsaves(nowEpochMillis: Long)

    /** Returns the rows finalized so callers can clean up their locally-copied images. */
    @Transaction
    open suspend fun finalizeExpiredPendingUnsaves(nowEpochMillis: Long): List<ArticleEntity> {
        val expired = selectExpiredPendingUnsaves(nowEpochMillis)
        if (expired.isNotEmpty()) clearExpiredPendingUnsaves(nowEpochMillis)
        return expired
    }

    @Query(
        """
        DELETE FROM articles
        WHERE isSaved = 0
          AND fetchedAtEpochMillis < :cutoffEpochMillis
        """,
    )
    abstract suspend fun deleteUnsavedOlderThan(cutoffEpochMillis: Long): Int
}

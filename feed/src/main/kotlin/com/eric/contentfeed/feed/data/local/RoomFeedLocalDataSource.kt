package com.eric.contentfeed.feed.data.local

import com.eric.contentfeed.core.database.ArticleDao
import com.eric.contentfeed.core.database.ArticleEntity
import com.eric.contentfeed.core.database.FeedPlacementDao
import com.eric.contentfeed.core.database.FeedPlacementEntity
import com.eric.contentfeed.core.database.WeatherDao
import com.eric.contentfeed.feed.domain.model.Article
import com.eric.contentfeed.feed.domain.model.CachedArticle
import com.eric.contentfeed.feed.domain.model.FeedPlacement
import com.eric.contentfeed.feed.domain.model.WeatherData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomFeedLocalDataSource(
    private val articleDao: ArticleDao,
    private val weatherDao: WeatherDao,
    private val feedPlacementDao: FeedPlacementDao,
    private val articleAuthorsJsonCodec: ArticleAuthorsJsonCodec,
    private val weatherCacheJsonCodec: WeatherCacheJsonCodec,
) : FeedLocalDataSource {
    override fun observeArticles(): Flow<List<CachedArticle>> =
        articleDao.observeArticles().map { articles -> articles.map(::toCachedArticle) }

    override fun observeSavedArticles(): Flow<List<CachedArticle>> =
        articleDao.observeSavedArticles().map { articles -> articles.map(::toCachedArticle) }

    override fun observeArticle(articleId: Int): Flow<CachedArticle?> =
        articleDao.observeArticle(articleId).map { article -> article?.let(::toCachedArticle) }

    override fun observeWeather(): Flow<WeatherData?> =
        weatherDao.observeWeather().map { cache -> cache?.let(weatherCacheJsonCodec::decode) }

    override fun observePlacements(): Flow<List<FeedPlacement>> =
        feedPlacementDao.observePlacements().map { placements -> placements.map(::toDomainPlacement) }

    override suspend fun countPlacementsByContentType(contentType: String): Int =
        feedPlacementDao.countPlacementsByContentType(contentType)

    override suspend fun upsertArticles(
        articles: List<Article>,
        fetchedAtEpochMillis: Long,
    ) {
        articleDao.upsertRemoteArticles(
            articles.map { article ->
                ArticleEntity(
                    id = article.id,
                    title = article.title,
                    source = article.source,
                    authorsJson = articleAuthorsJsonCodec.encode(article.authors),
                    summary = article.summary,
                    imageUrl = article.imageUrl,
                    articleUrl = article.articleUrl,
                    publishedAtEpochMillis = article.publishedAtEpochMillis,
                    fetchedAtEpochMillis = fetchedAtEpochMillis,
                )
            },
        )
    }

    override suspend fun upsertWeather(weather: WeatherData) {
        weatherDao.upsertWeather(weatherCacheJsonCodec.encode(weather))
    }

    override suspend fun insertPlacement(placement: FeedPlacement) {
        feedPlacementDao.insertIfAbsent(
            FeedPlacementEntity(
                anchorArticleId = placement.anchorArticleId,
                anchorPublishedAtEpochMillis = placement.anchorPublishedAtEpochMillis,
                contentType = placement.contentType,
                poolIndex = placement.poolIndex,
                assignmentSequence = placement.assignmentSequence,
            ),
        )
    }

    override suspend fun saveArticle(
        articleId: Int,
        savedAtEpochMillis: Long,
        localImagePath: String?,
    ): Boolean = articleDao.saveArticle(articleId, savedAtEpochMillis, localImagePath) > 0

    override suspend fun unsaveArticleImmediately(articleId: Int) {
        articleDao.unsaveImmediate(articleId)
    }

    override suspend fun markPendingUnsave(
        articleId: Int,
        deadlineEpochMillis: Long,
    ): Boolean = articleDao.markPendingUnsave(articleId, deadlineEpochMillis) > 0

    override suspend fun undoUnsave(
        articleId: Int,
        nowEpochMillis: Long,
    ): Boolean = articleDao.undoPendingUnsave(articleId, nowEpochMillis) > 0

    override suspend fun finalizeExpiredPendingUnsaves(nowEpochMillis: Long): List<CachedArticle> =
        articleDao.finalizeExpiredPendingUnsaves(nowEpochMillis).map(::toCachedArticle)

    override suspend fun pruneUnsavedArticles(cutoffEpochMillis: Long): Int =
        articleDao.deleteUnsavedOlderThan(cutoffEpochMillis)

    private fun toCachedArticle(entity: ArticleEntity): CachedArticle =
        CachedArticle(
            article =
                Article(
                    id = entity.id,
                    title = entity.title,
                    source = entity.source,
                    authors = articleAuthorsJsonCodec.decode(entity.authorsJson),
                    summary = entity.summary,
                    imageUrl = entity.imageUrl,
                    articleUrl = entity.articleUrl,
                    publishedAtEpochMillis = entity.publishedAtEpochMillis,
                ),
            // Domain-visible saved state hides a pending-removal row immediately, even
            // though its data/image are retained in Room until the undo window lapses.
            isSaved = entity.isSaved && entity.pendingUnsaveAtEpochMillis == null,
            savedAtEpochMillis = entity.savedAtEpochMillis,
            localImagePath = entity.localImagePath,
            pendingUnsaveAtEpochMillis = entity.pendingUnsaveAtEpochMillis,
        )

    private fun toDomainPlacement(entity: FeedPlacementEntity): FeedPlacement =
        FeedPlacement(
            anchorArticleId = entity.anchorArticleId,
            anchorPublishedAtEpochMillis = entity.anchorPublishedAtEpochMillis,
            contentType = entity.contentType,
            poolIndex = entity.poolIndex,
            assignmentSequence = entity.assignmentSequence,
        )
}

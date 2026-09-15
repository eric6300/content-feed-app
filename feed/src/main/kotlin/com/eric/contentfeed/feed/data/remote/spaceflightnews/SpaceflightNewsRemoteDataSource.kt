package com.eric.contentfeed.feed.data.remote.spaceflightnews

import com.eric.contentfeed.feed.data.remote.ArticlePageResult
import com.eric.contentfeed.feed.data.remote.ArticleRemoteDataSource
import com.eric.contentfeed.feed.data.remote.SPACEFLIGHT_NEWS_ARTICLES_PATH
import com.eric.contentfeed.feed.data.remote.toRemoteFailure
import com.skydoves.sandwich.ApiResponse

internal class SpaceflightNewsRemoteDataSource(
    private val api: SpaceflightNewsApi,
) : ArticleRemoteDataSource {
    override suspend fun fetchPage(
        offset: Int,
        limit: Int,
    ): ArticlePageResult =
        when (
            val response =
                api.getArticles(url = SPACEFLIGHT_NEWS_ARTICLES_PATH, offset = offset, limit = limit)
        ) {
            is ApiResponse.Success ->
                ArticlePageResult.Loaded(
                    articles = response.data.toArticles(),
                    isLastPage = response.data.next == null,
                )
            is ApiResponse.Failure<*> -> ArticlePageResult.Failure(response.toRemoteFailure())
        }
}

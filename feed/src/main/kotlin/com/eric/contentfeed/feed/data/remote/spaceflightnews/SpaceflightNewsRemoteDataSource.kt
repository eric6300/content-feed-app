package com.eric.contentfeed.feed.data.remote.spaceflightnews

import com.eric.contentfeed.feed.data.remote.ArticlePage
import com.eric.contentfeed.feed.data.remote.ArticleRemoteDataSource
import com.eric.contentfeed.feed.data.remote.RemoteResult
import com.eric.contentfeed.feed.data.remote.bodyAs
import com.eric.contentfeed.feed.data.remote.toRemoteFailure
import com.eric.contentfeed.feed.domain.model.RemoteFailure
import com.skydoves.sandwich.ApiResponse

internal class SpaceflightNewsRemoteDataSource(
    private val api: SpaceflightNewsApi,
) : ArticleRemoteDataSource {
    override suspend fun fetchPage(
        offset: Int,
        limit: Int,
    ): RemoteResult<ArticlePage> =
        when (val response = api.getArticles(offset = offset, limit = limit)) {
            is ApiResponse.Success ->
                response
                    .bodyAs<ArticleListResponseDto>()
                    ?.let { RemoteResult.Loaded(ArticlePage(articles = it.toArticles(), isLastPage = it.next == null)) }
                    ?: RemoteResult.Failure(RemoteFailure.Unknown)
            is ApiResponse.Failure<*> -> RemoteResult.Failure(response.toRemoteFailure())
        }
}

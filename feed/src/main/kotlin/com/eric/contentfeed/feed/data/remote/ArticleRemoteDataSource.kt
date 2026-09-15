package com.eric.contentfeed.feed.data.remote

import com.eric.contentfeed.feed.domain.model.Article
import com.eric.contentfeed.feed.domain.model.RemoteFailure

interface ArticleRemoteDataSource {
    suspend fun fetchPage(
        offset: Int,
        limit: Int,
    ): ArticlePageResult
}

sealed interface ArticlePageResult {
    /** A successful empty first page and end-of-pagination are both [Loaded];
     * [isLastPage] comes from the source's own `next` link, never from comparing
     * [articles].size to the requested limit. */
    data class Loaded(
        val articles: List<Article>,
        val isLastPage: Boolean,
    ) : ArticlePageResult

    data class Failure(
        val cause: RemoteFailure,
    ) : ArticlePageResult
}

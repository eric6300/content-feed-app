package com.eric.contentfeed.feed.data.remote

import com.eric.contentfeed.feed.domain.model.Article

interface ArticleRemoteDataSource {
    suspend fun fetchPage(
        offset: Int,
        limit: Int,
    ): RemoteResult<ArticlePage>
}

/** A successful empty first page and end-of-pagination are both a `RemoteResult.Loaded`
 * [ArticlePage]; [isLastPage] comes from the source's own `next` link, never from
 * comparing [articles].size to the requested limit. */
data class ArticlePage(
    val articles: List<Article>,
    val isLastPage: Boolean,
)

package com.eric.contentfeed.feed.data.remote.spaceflightnews

import com.eric.contentfeed.feed.data.remote.SPACEFLIGHT_NEWS_ARTICLES_PATH
import com.skydoves.sandwich.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

internal interface SpaceflightNewsApi {
    @GET(SPACEFLIGHT_NEWS_ARTICLES_PATH)
    suspend fun getArticles(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
    ): ApiResponse<ArticleListResponseDto>
}

package com.eric.contentfeed.feed.data.remote.spaceflightnews

import com.skydoves.sandwich.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

internal interface SpaceflightNewsApi {
    @GET
    suspend fun getArticles(
        @Url url: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
    ): ApiResponse<ArticleListResponseDto>
}

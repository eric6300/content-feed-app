package com.eric.contentfeed.feed.data.remote.spaceflightnews

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class ArticleListResponseDto(
    val next: String?,
    val results: List<ArticleDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
internal data class ArticleDto(
    val id: Int,
    val title: String,
    val url: String,
    @Json(name = "news_site") val newsSite: String,
    @Json(name = "image_url") val imageUrl: String?,
    val summary: String?,
    @Json(name = "published_at") val publishedAt: String?,
    val authors: List<AuthorDto>?,
)

@JsonClass(generateAdapter = true)
internal data class AuthorDto(
    val name: String?,
)

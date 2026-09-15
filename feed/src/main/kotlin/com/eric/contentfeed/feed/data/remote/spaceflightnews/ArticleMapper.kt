package com.eric.contentfeed.feed.data.remote.spaceflightnews

import com.eric.contentfeed.feed.data.remote.IsoTimestampParser
import com.eric.contentfeed.feed.domain.model.Article

internal fun ArticleListResponseDto.toArticles(): List<Article> = results.map(ArticleDto::toArticle)

internal fun ArticleDto.toArticle(): Article =
    Article(
        id = id,
        title = title,
        source = newsSite,
        authors = authors.orEmpty().mapNotNull { it.name?.trim()?.ifEmpty { null } },
        summary = summary?.trim()?.ifEmpty { null },
        imageUrl = imageUrl?.trim()?.ifEmpty { null },
        articleUrl = url,
        publishedAtEpochMillis =
            IsoTimestampParser.parseToEpochMillis(publishedAt)?.takeIf { it != 0L },
    )

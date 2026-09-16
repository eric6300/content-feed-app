package com.eric.contentfeed.feed.presentation.model

import com.eric.contentfeed.feed.domain.model.CachedArticle

internal fun CachedArticle.toArticleUiModel(): ArticleUiModel =
    ArticleUiModel(
        id = article.id,
        title = article.title,
        source = article.source,
        authors = article.authors,
        summary = article.summary,
        imageUrl = article.imageUrl,
        articleUrl = article.articleUrl,
        publishedAtEpochMillis = article.publishedAtEpochMillis,
        isSaved = isSaved,
        localImagePath = localImagePath,
    )

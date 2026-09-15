package com.eric.contentfeed.feed.domain.usecase

import com.eric.contentfeed.feed.repository.ArticleRepository

/** Appends the next page of articles at the current pagination cursor. A no-op once
 * the stream is exhausted; a failed call leaves the cursor and cache untouched, so
 * invoking this again is the retry path (`USE_CASES.md` → "Loading the next page of
 * articles fails"). */
class LoadNextArticlePageUseCase(
    private val articleRepository: ArticleRepository,
) {
    suspend operator fun invoke() {
        articleRepository.loadNextPage()
    }
}

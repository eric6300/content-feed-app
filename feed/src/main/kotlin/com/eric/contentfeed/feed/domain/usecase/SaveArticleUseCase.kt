package com.eric.contentfeed.feed.domain.usecase

import com.eric.contentfeed.feed.repository.SavedArticleRepository

/** Saves an article through the shared feed/detail/Saved command path described in
 * `docs/USE_CASES.md` → Feature: Save for later. */
class SaveArticleUseCase(
    private val repository: SavedArticleRepository,
) {
    suspend operator fun invoke(articleId: Int) {
        repository.saveArticle(articleId)
    }
}

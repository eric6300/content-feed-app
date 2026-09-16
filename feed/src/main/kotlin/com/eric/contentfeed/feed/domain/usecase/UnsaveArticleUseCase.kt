package com.eric.contentfeed.feed.domain.usecase

import com.eric.contentfeed.feed.repository.SavedArticleRepository

/** The Saved-list path delays destructive cleanup; feed/detail toggles are immediate,
 * matching `docs/USE_CASES.md` → Feature: Unsave. */
enum class UnsaveSource {
    FeedOrDetail,
    SavedList,
}

class UnsaveArticleUseCase(
    private val repository: SavedArticleRepository,
) {
    suspend operator fun invoke(
        articleId: Int,
        source: UnsaveSource,
    ): Boolean {
        when (source) {
            UnsaveSource.FeedOrDetail -> {
                repository.unsaveArticleImmediately(articleId)
                return true
            }
            UnsaveSource.SavedList -> return repository.removeFromSavedList(articleId)
        }
    }
}

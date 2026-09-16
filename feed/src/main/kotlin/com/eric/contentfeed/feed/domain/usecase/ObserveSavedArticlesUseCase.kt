package com.eric.contentfeed.feed.domain.usecase

import com.eric.contentfeed.feed.domain.model.CachedArticle
import com.eric.contentfeed.feed.repository.SavedArticleRepository
import kotlinx.coroutines.flow.Flow

/** Observes Room-backed Saved-list content, including offline reads; see
 * `docs/USE_CASES.md` → Feature: Offline access to saved items. */
class ObserveSavedArticlesUseCase(
    private val repository: SavedArticleRepository,
) {
    operator fun invoke(): Flow<List<CachedArticle>> = repository.observeSavedArticles()
}

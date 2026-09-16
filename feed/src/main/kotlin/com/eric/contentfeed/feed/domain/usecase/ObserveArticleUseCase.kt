package com.eric.contentfeed.feed.domain.usecase

import com.eric.contentfeed.feed.domain.model.CachedArticle
import com.eric.contentfeed.feed.repository.SavedArticleRepository
import kotlinx.coroutines.flow.Flow

/** Observes cached article detail content for offline Saved reading; see
 * `docs/USE_CASES.md` → Feature: Offline access to saved items. */
class ObserveArticleUseCase(
    private val repository: SavedArticleRepository,
) {
    operator fun invoke(articleId: Int): Flow<CachedArticle?> = repository.observeArticle(articleId)
}

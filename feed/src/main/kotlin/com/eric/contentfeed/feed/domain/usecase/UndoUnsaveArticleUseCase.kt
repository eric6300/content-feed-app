package com.eric.contentfeed.feed.domain.usecase

import com.eric.contentfeed.feed.repository.SavedArticleRepository

/** Restores a Saved-list removal while its persisted undo deadline remains open; see
 * `docs/USE_CASES.md` → Feature: Unsave. */
class UndoUnsaveArticleUseCase(
    private val repository: SavedArticleRepository,
) {
    suspend operator fun invoke(articleId: Int): Boolean = repository.undoRemoval(articleId)
}

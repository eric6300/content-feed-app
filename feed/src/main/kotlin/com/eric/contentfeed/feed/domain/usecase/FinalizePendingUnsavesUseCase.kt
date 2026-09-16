package com.eric.contentfeed.feed.domain.usecase

import com.eric.contentfeed.feed.repository.SavedArticleRepository

/** Identifies the caller-driven points where pending removals are finalized. */
sealed interface FinalizeTrigger {
    data object AppStart : FinalizeTrigger

    data class UndoWindowElapsed(
        val articleId: Int,
    ) : FinalizeTrigger
}

/** Finalizes pending removals without owning a timer or coroutine scope; see
 * `docs/USE_CASES.md` → Feature: Unsave. Snackbar dismissal finalizes only its
 * article, while app start sweeps every pending removal. */
class FinalizePendingUnsavesUseCase(
    private val repository: SavedArticleRepository,
) {
    suspend operator fun invoke(trigger: FinalizeTrigger) {
        when (trigger) {
            FinalizeTrigger.AppStart -> repository.finalizeAllPendingRemovals()
            is FinalizeTrigger.UndoWindowElapsed -> repository.finalizeRemoval(trigger.articleId)
        }
    }
}

package com.eric.contentfeed.feed.domain.usecase

import com.eric.contentfeed.feed.repository.SavedArticleRepository

/** Identifies the caller-driven points where pending removals are finalized. */
sealed interface FinalizeTrigger {
    /** Process cold start: no pending undo window can still be live in the UI, so every
     * pending removal is finalized regardless of its deadline. */
    data object AppStart : FinalizeTrigger

    /** The app returned to the foreground after being backgrounded. Unlike [AppStart],
     * a pending removal's Undo snackbar may still be live in the UI, so only deadlines
     * that have actually elapsed are finalized. */
    data object ForegroundReturn : FinalizeTrigger

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
            FinalizeTrigger.ForegroundReturn -> repository.finalizeExpiredRemovals()
            is FinalizeTrigger.UndoWindowElapsed -> repository.finalizeRemoval(trigger.articleId)
        }
    }
}

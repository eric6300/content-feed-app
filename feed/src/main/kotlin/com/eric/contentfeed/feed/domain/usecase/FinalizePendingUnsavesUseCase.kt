package com.eric.contentfeed.feed.domain.usecase

import com.eric.contentfeed.feed.repository.SavedArticleRepository

/** Identifies the caller-driven points where pending removals are finalized. */
enum class FinalizeTrigger {
    AppStart,
    UndoWindowElapsed,
}

/** Finalizes pending removals without owning a timer or coroutine scope; see
 * `docs/USE_CASES.md` → Feature: Unsave. */
class FinalizePendingUnsavesUseCase(
    private val repository: SavedArticleRepository,
) {
    suspend operator fun invoke(trigger: FinalizeTrigger) {
        when (trigger) {
            FinalizeTrigger.AppStart -> repository.finalizeAllPendingRemovals()
            FinalizeTrigger.UndoWindowElapsed -> repository.finalizeExpiredRemovals()
        }
    }
}

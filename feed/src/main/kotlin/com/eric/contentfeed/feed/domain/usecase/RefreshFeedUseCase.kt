package com.eric.contentfeed.feed.domain.usecase

import com.eric.contentfeed.feed.domain.model.FeedRefreshResult
import com.eric.contentfeed.feed.domain.model.SourceRefreshResult
import com.eric.contentfeed.feed.repository.ArticleRepository
import com.eric.contentfeed.feed.repository.RefreshOutcome
import com.eric.contentfeed.feed.repository.WeatherRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** What triggered a refresh. One trigger-parameterised use case beats three
 * near-identical ones — see `docs/IMPLEMENTATION_PLAN.md` → T4. */
enum class RefreshTrigger {
    /** Bypasses the freshness TTL for both sources. */
    Manual,

    /** Respects the TTL. Only trigger that can run retention cleanup, and only when
     * its own article refresh actually ran and succeeded. */
    InitialOpen,

    /** Respects the TTL. Never runs retention cleanup — connectivity can return in
     * the middle of an active scroll session, so pruning here could remove an article
     * the user is currently looking at (`USE_CASES.md` → "A manual pull-to-refresh or
     * a reconnect refresh does not run retention cleanup"). */
    Reconnect,

    /** Respects the TTL. Never runs retention cleanup, for the same reason as
     * [Reconnect]: a foreground return can land mid-scroll, and the app preserves
     * scroll position across it. */
    ForegroundReturn,
}

/** Refreshes weather and articles concurrently and independently — one source
 * failing must never block or roll back the other. */
class RefreshFeedUseCase(
    private val articleRepository: ArticleRepository,
    private val weatherRepository: WeatherRepository,
) {
    suspend operator fun invoke(trigger: RefreshTrigger): FeedRefreshResult {
        val bypassFreshness = trigger == RefreshTrigger.Manual

        val (articleOutcome, weatherOutcome) =
            coroutineScope {
                val weatherRefresh = async { weatherRepository.refresh(bypassFreshness) }
                val outcome = articleRepository.refreshTop(bypassFreshness)
                outcome to weatherRefresh.await()
            }

        if (trigger == RefreshTrigger.InitialOpen && articleOutcome == RefreshOutcome.Succeeded) {
            articleRepository.pruneStaleUnsavedArticles()
        }

        return FeedRefreshResult(
            articles = articleOutcome.toPresentationResult(),
            weather = weatherOutcome.toPresentationResult(),
        )
    }

    private fun RefreshOutcome.toPresentationResult(): SourceRefreshResult =
        when (this) {
            RefreshOutcome.Skipped -> SourceRefreshResult.Skipped
            RefreshOutcome.Succeeded -> SourceRefreshResult.Succeeded
            is RefreshOutcome.Failed -> SourceRefreshResult.Failed(cause)
        }
}

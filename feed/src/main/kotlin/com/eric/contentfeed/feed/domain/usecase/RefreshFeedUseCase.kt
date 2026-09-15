package com.eric.contentfeed.feed.domain.usecase

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
}

/** Refreshes weather and articles concurrently and independently — one source
 * failing must never block or roll back the other. */
class RefreshFeedUseCase(
    private val articleRepository: ArticleRepository,
    private val weatherRepository: WeatherRepository,
) {
    suspend operator fun invoke(trigger: RefreshTrigger) {
        val bypassFreshness = trigger == RefreshTrigger.Manual

        val articleOutcome =
            coroutineScope {
                val weatherRefresh = async { weatherRepository.refresh(bypassFreshness) }
                val outcome = articleRepository.refreshTop(bypassFreshness)
                weatherRefresh.await()
                outcome
            }

        if (trigger == RefreshTrigger.InitialOpen && articleOutcome == RefreshOutcome.Succeeded) {
            articleRepository.pruneStaleUnsavedArticles()
        }
    }
}

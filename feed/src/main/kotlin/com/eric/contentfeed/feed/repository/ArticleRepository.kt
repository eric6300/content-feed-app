package com.eric.contentfeed.feed.repository

import com.eric.contentfeed.feed.domain.model.CachedArticle
import com.eric.contentfeed.feed.domain.model.FeedPlacement
import com.eric.contentfeed.feed.domain.model.SourceStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Articles are their own repository, split from weather, so one source's failure is
 * structural rather than a matter of where a `try` happens to sit (see
 * `docs/IMPLEMENTATION_PLAN.md` → T4 style contract). Owns the top-of-stream
 * freshness refresh, page append, sticky service-card placement assignment, and the
 * 7-day unsaved-article retention cleanup. */
interface ArticleRepository {
    fun observeArticles(): Flow<List<CachedArticle>>

    fun observePlacements(): Flow<List<FeedPlacement>>

    val refreshStatus: StateFlow<SourceStatus>

    val appendStatus: StateFlow<SourceStatus>

    val isLastPage: StateFlow<Boolean>

    /** Top-of-stream freshness refresh: always requests offset zero, discovers new
     * articles by comparing ids against the current cache, assigns any new sticky
     * placements, and advances the pagination cursor by the number newly discovered.
     * [bypassFreshness] is `true` for a manual pull-to-refresh, `false` for the
     * initial-open/return and reconnect checks, which respect the TTL. */
    suspend fun refreshTop(bypassFreshness: Boolean): RefreshOutcome

    /** Appends the next page at the persisted cursor. A no-op once [isLastPage] is
     * true. Never bypassed by freshness — pagination is scroll-driven, not TTL-gated. */
    suspend fun loadNextPage()

    /** Runs the 7-day unsaved-article retention cleanup and removes placements
     * orphaned by it. Callers must only invoke this after a successful
     * initial-open/return top refresh — never after a manual or reconnect refresh
     * (`USE_CASES.md` → "Retention cleanup runs only from the initial open/return
     * freshness check"). */
    suspend fun pruneStaleUnsavedArticles()
}

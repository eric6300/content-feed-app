package com.eric.contentfeed.feed.repository

import com.eric.contentfeed.core.freshness.FreshnessGate
import com.eric.contentfeed.feed.data.remote.RemoteResult
import com.eric.contentfeed.feed.domain.model.RemoteFailure
import kotlin.time.Duration

/** Whether a refresh actually reached the network, distinguishing a stale check that
 * skipped the fetch entirely from one that fetched and failed — [RefreshFeedUseCase]
 * needs this distinction to decide whether retention cleanup should run at all. */
sealed interface RefreshOutcome {
    data object Skipped : RefreshOutcome

    data object Succeeded : RefreshOutcome

    data class Failed(
        val cause: RemoteFailure,
    ) : RefreshOutcome
}

/**
 * Collapses "check stale → fetch → persist → mark fetched" into one call, shared by
 * [DefaultArticleRepository] and [DefaultWeatherRepository]. Lives in `:feed`, not
 * next to [FreshnessGate] in `:core`, because [RemoteResult] is a `:feed`-only type —
 * a refinement of the `FreshnessGate` design recorded in `DECISIONS.md`, which left
 * this exact per-source refresh helper's location open.
 *
 * [FreshnessGate.markFetched] runs only when [fetch] succeeds: a failed refresh must
 * leave the source stale so the next automatic refresh retries it, rather than being
 * silenced for a full TTL (`USE_CASES.md` → "A failed source can be retried").
 */
internal class SourceRefresher(
    private val freshnessGate: FreshnessGate,
) {
    suspend fun <T> refreshIfNeeded(
        key: String,
        ttl: Duration,
        bypassFreshness: Boolean,
        fetch: suspend () -> RemoteResult<T>,
        persist: suspend (T) -> Unit,
    ): RefreshOutcome {
        if (!bypassFreshness && !freshnessGate.isStale(key, ttl)) return RefreshOutcome.Skipped

        return when (val result = fetch()) {
            is RemoteResult.Loaded -> {
                persist(result.value)
                freshnessGate.markFetched(key)
                RefreshOutcome.Succeeded
            }
            is RemoteResult.Failure -> RefreshOutcome.Failed(result.cause)
        }
    }
}

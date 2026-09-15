package com.eric.contentfeed.feed.repository

import com.eric.contentfeed.feed.domain.model.SourceStatus
import com.eric.contentfeed.feed.domain.model.WeatherData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Weather is its own repository, split from articles, so one source's failure is
 * structural rather than a matter of where a `try` happens to sit (see
 * `docs/IMPLEMENTATION_PLAN.md` → T4 style contract). */
interface WeatherRepository {
    fun observeWeather(): Flow<WeatherData?>

    val status: StateFlow<SourceStatus>

    /** [bypassFreshness] is `true` for a manual pull-to-refresh, `false` for the
     * initial-open/return and reconnect checks, which respect the TTL. */
    suspend fun refresh(bypassFreshness: Boolean): RefreshOutcome
}

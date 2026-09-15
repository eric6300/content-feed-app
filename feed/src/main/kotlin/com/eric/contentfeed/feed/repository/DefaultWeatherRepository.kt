package com.eric.contentfeed.feed.repository

import com.eric.contentfeed.core.freshness.FreshnessGate
import com.eric.contentfeed.feed.data.local.FeedLocalDataSource
import com.eric.contentfeed.feed.data.remote.WeatherRemoteDataSource
import com.eric.contentfeed.feed.domain.model.SourceStatus
import com.eric.contentfeed.feed.domain.model.WeatherData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class DefaultWeatherRepository(
    private val localDataSource: FeedLocalDataSource,
    private val remoteDataSource: WeatherRemoteDataSource,
    freshnessGate: FreshnessGate,
) : WeatherRepository {
    private val refresher = SourceRefresher(freshnessGate)
    private val mutableStatus = MutableStateFlow<SourceStatus>(SourceStatus.Idle)
    override val status: StateFlow<SourceStatus> = mutableStatus.asStateFlow()

    override fun observeWeather(): Flow<WeatherData?> = localDataSource.observeWeather()

    override suspend fun refresh(bypassFreshness: Boolean): RefreshOutcome {
        mutableStatus.value = SourceStatus.Loading
        val outcome =
            refresher.refreshIfNeeded(
                key = FeedPolicy.WEATHER_FRESHNESS_KEY,
                ttl = FeedPolicy.WEATHER_TTL,
                bypassFreshness = bypassFreshness,
                fetch = remoteDataSource::fetchCurrentAndForecast,
                persist = localDataSource::upsertWeather,
            )
        mutableStatus.value =
            when (outcome) {
                RefreshOutcome.Skipped, RefreshOutcome.Succeeded -> SourceStatus.Ready
                is RefreshOutcome.Failed -> SourceStatus.Failed(outcome.cause)
            }
        return outcome
    }
}

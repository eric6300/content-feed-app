package com.eric.contentfeed.feed.repository

import com.eric.contentfeed.core.freshness.FreshnessGate
import com.eric.contentfeed.feed.data.local.FeedLocalDataSource
import com.eric.contentfeed.feed.data.remote.RemoteResult
import com.eric.contentfeed.feed.data.remote.WeatherRemoteDataSource
import com.eric.contentfeed.feed.domain.model.RemoteFailure
import com.eric.contentfeed.feed.domain.model.SourceStatus
import com.eric.contentfeed.feed.domain.model.WeatherData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DefaultWeatherRepositoryTest {
    private lateinit var localDataSource: FeedLocalDataSource
    private lateinit var remoteDataSource: WeatherRemoteDataSource
    private lateinit var freshnessGate: FreshnessGate
    private lateinit var repository: DefaultWeatherRepository

    private val weather =
        WeatherData(
            temperatureCelsius = 25.0,
            apparentTemperatureCelsius = 26.0,
            weatherCode = 0,
            windSpeedKmh = 5.0,
            forecast = emptyList(),
        )

    @Before
    fun setUp() {
        localDataSource = mockk(relaxed = true)
        remoteDataSource = mockk()
        freshnessGate = mockk(relaxed = true)
        every { localDataSource.observeWeather() } returns flowOf(weather)
        repository = DefaultWeatherRepository(localDataSource, remoteDataSource, freshnessGate)
    }

    @Test
    fun freshCacheIsServedWithoutFetching() =
        runTest {
            coEvery { freshnessGate.isStale(FeedPolicy.WEATHER_FRESHNESS_KEY, FeedPolicy.WEATHER_TTL) } returns false

            val outcome = repository.refresh(bypassFreshness = false)

            assertEquals(RefreshOutcome.Skipped, outcome)
            assertEquals(SourceStatus.Ready, repository.status.value)
            coVerify(exactly = 0) { remoteDataSource.fetchCurrentAndForecast() }
        }

    @Test
    fun staleCacheTriggersRefetchAndMarksFetched() =
        runTest {
            coEvery { freshnessGate.isStale(any(), any()) } returns true
            coEvery { remoteDataSource.fetchCurrentAndForecast() } returns RemoteResult.Loaded(weather)

            val outcome = repository.refresh(bypassFreshness = false)

            assertEquals(RefreshOutcome.Succeeded, outcome)
            assertEquals(SourceStatus.Ready, repository.status.value)
            coVerify { localDataSource.upsertWeather(weather) }
            coVerify { freshnessGate.markFetched(FeedPolicy.WEATHER_FRESHNESS_KEY) }
        }

    @Test
    fun failureDoesNotMarkFetchedAndSurfacesFailedStatus() =
        runTest {
            coEvery { freshnessGate.isStale(any(), any()) } returns true
            coEvery { remoteDataSource.fetchCurrentAndForecast() } returns
                RemoteResult.Failure(RemoteFailure.NetworkUnavailable)

            val outcome = repository.refresh(bypassFreshness = false)

            assertEquals(RefreshOutcome.Failed(RemoteFailure.NetworkUnavailable), outcome)
            assertEquals(SourceStatus.Failed(RemoteFailure.NetworkUnavailable), repository.status.value)
            coVerify(exactly = 0) { freshnessGate.markFetched(any()) }
            coVerify(exactly = 0) { localDataSource.upsertWeather(any()) }
        }

    @Test
    fun manualBypassFetchesRegardlessOfFreshness() =
        runTest {
            coEvery { freshnessGate.isStale(any(), any()) } returns false
            coEvery { remoteDataSource.fetchCurrentAndForecast() } returns RemoteResult.Loaded(weather)

            val outcome = repository.refresh(bypassFreshness = true)

            assertEquals(RefreshOutcome.Succeeded, outcome)
            coVerify { remoteDataSource.fetchCurrentAndForecast() }
        }
}

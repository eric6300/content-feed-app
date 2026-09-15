package com.eric.contentfeed.feed.data.remote.openmeteo

import com.eric.contentfeed.feed.data.remote.OPEN_METEO_FORECAST_PATH
import com.eric.contentfeed.feed.data.remote.WEATHER_CURRENT_FIELDS
import com.eric.contentfeed.feed.data.remote.WEATHER_DAILY_FIELDS
import com.eric.contentfeed.feed.data.remote.WEATHER_FORECAST_DAYS
import com.eric.contentfeed.feed.data.remote.WEATHER_LATITUDE
import com.eric.contentfeed.feed.data.remote.WEATHER_LONGITUDE
import com.eric.contentfeed.feed.data.remote.WEATHER_TIMEZONE
import com.eric.contentfeed.feed.data.remote.WeatherFetchResult
import com.eric.contentfeed.feed.domain.model.RemoteFailure
import com.skydoves.sandwich.ApiResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.net.UnknownHostException

class OpenMeteoRemoteDataSourceTest {
    private lateinit var api: OpenMeteoApi
    private lateinit var dataSource: OpenMeteoRemoteDataSource

    @Before
    fun setUp() {
        api = mockk()
        dataSource = OpenMeteoRemoteDataSource(api)
    }

    @Test
    fun successMapsToLoadedWeather() =
        runTest {
            val dto = OpenMeteoForecastResponseDto(current = OpenMeteoCurrentDto(24.5, 27.1, 53, 2.7), daily = null)
            coEvery { api.getForecast(any(), any(), any(), any(), any(), any(), any()) } returns
                ApiResponse.Success(dto)

            val result = dataSource.fetchCurrentAndForecast() as WeatherFetchResult.Loaded

            assertEquals(24.5, result.weather.temperatureCelsius)
        }

    @Test
    fun httpFailureMapsToFailureResult() =
        runTest {
            coEvery { api.getForecast(any(), any(), any(), any(), any(), any(), any()) } returns
                ApiResponse.Failure.Exception(UnknownHostException())

            val result = dataSource.fetchCurrentAndForecast() as WeatherFetchResult.Failure

            assertEquals(RemoteFailure.NetworkUnavailable, result.cause)
        }

    @Test
    fun queryArgumentsComeFromRemoteSourceConfig() =
        runTest {
            val dto = OpenMeteoForecastResponseDto(current = null, daily = null)
            coEvery { api.getForecast(any(), any(), any(), any(), any(), any(), any()) } returns
                ApiResponse.Success(dto)

            dataSource.fetchCurrentAndForecast()

            coVerify {
                api.getForecast(
                    url = OPEN_METEO_FORECAST_PATH,
                    latitude = WEATHER_LATITUDE,
                    longitude = WEATHER_LONGITUDE,
                    current = WEATHER_CURRENT_FIELDS,
                    daily = WEATHER_DAILY_FIELDS,
                    timezone = WEATHER_TIMEZONE,
                    forecastDays = WEATHER_FORECAST_DAYS,
                )
            }
        }
}

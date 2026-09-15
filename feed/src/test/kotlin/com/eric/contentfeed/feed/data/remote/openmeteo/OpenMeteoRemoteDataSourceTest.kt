package com.eric.contentfeed.feed.data.remote.openmeteo

import com.eric.contentfeed.feed.data.remote.RemoteResult
import com.eric.contentfeed.feed.data.remote.WEATHER_CURRENT_FIELDS
import com.eric.contentfeed.feed.data.remote.WEATHER_DAILY_FIELDS
import com.eric.contentfeed.feed.data.remote.WEATHER_FORECAST_DAYS
import com.eric.contentfeed.feed.data.remote.WEATHER_LATITUDE
import com.eric.contentfeed.feed.data.remote.WEATHER_LONGITUDE
import com.eric.contentfeed.feed.data.remote.WEATHER_TIMEZONE
import com.eric.contentfeed.feed.domain.model.RemoteFailure
import com.eric.contentfeed.feed.domain.model.WeatherData
import com.skydoves.sandwich.ApiResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Response
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
            coEvery { api.getForecast(any(), any(), any(), any(), any(), any()) } returns
                ApiResponse.Success(dto)

            val result = dataSource.fetchCurrentAndForecast()

            assertEquals(24.5, (result as RemoteResult.Loaded<WeatherData>).value.temperatureCelsius)
        }

    @Test
    fun networkExceptionMapsToFailureResult() =
        runTest {
            coEvery { api.getForecast(any(), any(), any(), any(), any(), any()) } returns
                ApiResponse.Failure.Exception(UnknownHostException())

            val result = dataSource.fetchCurrentAndForecast()

            assertEquals(RemoteResult.Failure(RemoteFailure.NetworkUnavailable), result)
        }

    @Test
    fun httpErrorMapsToFailureResultWithTheRealStatusCode() =
        runTest {
            coEvery { api.getForecast(any(), any(), any(), any(), any(), any()) } returns
                ApiResponse.Failure.Error(errorResponse(503))

            val result = dataSource.fetchCurrentAndForecast()

            assertEquals(RemoteResult.Failure(RemoteFailure.Http(503)), result)
        }

    @Test
    fun aSuccessCarryingNoBodyMapsToUnknownFailure() =
        runTest {
            @Suppress("UNCHECKED_CAST")
            coEvery { api.getForecast(any(), any(), any(), any(), any(), any()) } returns
                ApiResponse.Success<Any>(Unit) as ApiResponse.Success<OpenMeteoForecastResponseDto>

            val result = dataSource.fetchCurrentAndForecast()

            assertEquals(RemoteResult.Failure(RemoteFailure.Unknown), result)
        }

    @Test
    fun queryArgumentsComeFromRemoteSourceConfig() =
        runTest {
            val dto = OpenMeteoForecastResponseDto(current = null, daily = null)
            coEvery { api.getForecast(any(), any(), any(), any(), any(), any()) } returns
                ApiResponse.Success(dto)

            dataSource.fetchCurrentAndForecast()

            coVerify {
                api.getForecast(
                    latitude = WEATHER_LATITUDE,
                    longitude = WEATHER_LONGITUDE,
                    current = WEATHER_CURRENT_FIELDS,
                    daily = WEATHER_DAILY_FIELDS,
                    timezone = WEATHER_TIMEZONE,
                    forecastDays = WEATHER_FORECAST_DAYS,
                )
            }
        }

    private fun errorResponse(code: Int): Response<*> =
        Response.error<Any>(
            code,
            "".toResponseBody("application/json".toMediaType()),
        )
}

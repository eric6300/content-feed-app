package com.eric.contentfeed.feed.data.remote.openmeteo

import com.eric.contentfeed.feed.data.remote.RemoteResult
import com.eric.contentfeed.feed.data.remote.WEATHER_CURRENT_FIELDS
import com.eric.contentfeed.feed.data.remote.WEATHER_DAILY_FIELDS
import com.eric.contentfeed.feed.data.remote.WEATHER_FORECAST_DAYS
import com.eric.contentfeed.feed.data.remote.WEATHER_LATITUDE
import com.eric.contentfeed.feed.data.remote.WEATHER_LONGITUDE
import com.eric.contentfeed.feed.data.remote.WEATHER_TIMEZONE
import com.eric.contentfeed.feed.data.remote.WeatherRemoteDataSource
import com.eric.contentfeed.feed.data.remote.bodyAs
import com.eric.contentfeed.feed.data.remote.toRemoteFailure
import com.eric.contentfeed.feed.domain.model.RemoteFailure
import com.eric.contentfeed.feed.domain.model.WeatherData
import com.skydoves.sandwich.ApiResponse

internal class OpenMeteoRemoteDataSource(
    private val api: OpenMeteoApi,
) : WeatherRemoteDataSource {
    override suspend fun fetchCurrentAndForecast(): RemoteResult<WeatherData> {
        val response =
            api.getForecast(
                latitude = WEATHER_LATITUDE,
                longitude = WEATHER_LONGITUDE,
                current = WEATHER_CURRENT_FIELDS,
                daily = WEATHER_DAILY_FIELDS,
                timezone = WEATHER_TIMEZONE,
                forecastDays = WEATHER_FORECAST_DAYS,
            )
        return when (response) {
            is ApiResponse.Success ->
                response
                    .bodyAs<OpenMeteoForecastResponseDto>()
                    ?.let { RemoteResult.Loaded(it.toWeatherData()) }
                    ?: RemoteResult.Failure(RemoteFailure.Unknown)
            is ApiResponse.Failure<*> -> RemoteResult.Failure(response.toRemoteFailure())
        }
    }
}

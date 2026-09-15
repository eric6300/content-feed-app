package com.eric.contentfeed.feed.data.remote.openmeteo

import com.eric.contentfeed.feed.data.remote.OPEN_METEO_FORECAST_PATH
import com.eric.contentfeed.feed.data.remote.WEATHER_CURRENT_FIELDS
import com.eric.contentfeed.feed.data.remote.WEATHER_DAILY_FIELDS
import com.eric.contentfeed.feed.data.remote.WEATHER_FORECAST_DAYS
import com.eric.contentfeed.feed.data.remote.WEATHER_LATITUDE
import com.eric.contentfeed.feed.data.remote.WEATHER_LONGITUDE
import com.eric.contentfeed.feed.data.remote.WEATHER_TIMEZONE
import com.eric.contentfeed.feed.data.remote.WeatherFetchResult
import com.eric.contentfeed.feed.data.remote.WeatherRemoteDataSource
import com.eric.contentfeed.feed.data.remote.toRemoteFailure
import com.skydoves.sandwich.ApiResponse

internal class OpenMeteoRemoteDataSource(
    private val api: OpenMeteoApi,
) : WeatherRemoteDataSource {
    override suspend fun fetchCurrentAndForecast(): WeatherFetchResult {
        val response =
            api.getForecast(
                url = OPEN_METEO_FORECAST_PATH,
                latitude = WEATHER_LATITUDE,
                longitude = WEATHER_LONGITUDE,
                current = WEATHER_CURRENT_FIELDS,
                daily = WEATHER_DAILY_FIELDS,
                timezone = WEATHER_TIMEZONE,
                forecastDays = WEATHER_FORECAST_DAYS,
            )
        return when (response) {
            is ApiResponse.Success -> WeatherFetchResult.Loaded(response.data.toWeatherData())
            is ApiResponse.Failure<*> -> WeatherFetchResult.Failure(response.toRemoteFailure())
        }
    }
}

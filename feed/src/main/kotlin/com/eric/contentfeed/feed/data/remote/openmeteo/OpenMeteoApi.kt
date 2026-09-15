package com.eric.contentfeed.feed.data.remote.openmeteo

import com.eric.contentfeed.feed.data.remote.OPEN_METEO_FORECAST_PATH
import com.skydoves.sandwich.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

internal interface OpenMeteoApi {
    @GET(OPEN_METEO_FORECAST_PATH)
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String,
        @Query("daily") daily: String,
        @Query("timezone") timezone: String,
        @Query("forecast_days") forecastDays: Int,
    ): ApiResponse<OpenMeteoForecastResponseDto>
}

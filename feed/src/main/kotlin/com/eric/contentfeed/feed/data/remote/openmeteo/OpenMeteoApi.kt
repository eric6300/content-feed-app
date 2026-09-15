package com.eric.contentfeed.feed.data.remote.openmeteo

import com.skydoves.sandwich.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

internal interface OpenMeteoApi {
    @GET
    suspend fun getForecast(
        @Url url: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String,
        @Query("daily") daily: String,
        @Query("timezone") timezone: String,
        @Query("forecast_days") forecastDays: Int,
    ): ApiResponse<OpenMeteoForecastResponseDto>
}

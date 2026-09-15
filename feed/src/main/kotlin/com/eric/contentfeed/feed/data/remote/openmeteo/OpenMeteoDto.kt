package com.eric.contentfeed.feed.data.remote.openmeteo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class OpenMeteoForecastResponseDto(
    val current: OpenMeteoCurrentDto?,
    val daily: OpenMeteoDailyDto?,
)

@JsonClass(generateAdapter = true)
internal data class OpenMeteoCurrentDto(
    @Json(name = "temperature_2m") val temperature2m: Double?,
    @Json(name = "apparent_temperature") val apparentTemperature: Double?,
    @Json(name = "weather_code") val weatherCode: Int?,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double?,
)

@JsonClass(generateAdapter = true)
internal data class OpenMeteoDailyDto(
    val time: List<String>?,
    @Json(name = "temperature_2m_max") val temperatureMax: List<Double?>?,
    @Json(name = "temperature_2m_min") val temperatureMin: List<Double?>?,
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int?>?,
    @Json(name = "weather_code") val weatherCode: List<Int?>?,
)

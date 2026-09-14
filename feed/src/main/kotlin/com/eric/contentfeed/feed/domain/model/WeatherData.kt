package com.eric.contentfeed.feed.domain.model

data class WeatherData(
    val temperatureCelsius: Double?,
    val apparentTemperatureCelsius: Double?,
    val weatherCode: Int?,
    val windSpeedKmh: Double?,
    val forecast: List<WeatherForecastDay>,
)

data class WeatherForecastDay(
    val date: String,
    val temperatureMaxCelsius: Double?,
    val temperatureMinCelsius: Double?,
    val precipitationProbability: Int?,
    val weatherCode: Int?,
)

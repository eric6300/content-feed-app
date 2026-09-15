package com.eric.contentfeed.feed.data.remote.openmeteo

import com.eric.contentfeed.feed.domain.model.WeatherData
import com.eric.contentfeed.feed.domain.model.WeatherForecastDay

internal fun OpenMeteoForecastResponseDto.toWeatherData(): WeatherData =
    WeatherData(
        temperatureCelsius = current?.temperature2m,
        apparentTemperatureCelsius = current?.apparentTemperature,
        weatherCode = current?.weatherCode,
        windSpeedKmh = current?.windSpeed10m,
        forecast = daily?.toForecastDays().orEmpty(),
    )

private fun OpenMeteoDailyDto.toForecastDays(): List<WeatherForecastDay> =
    time.orEmpty().mapIndexed { index, date ->
        WeatherForecastDay(
            date = date,
            temperatureMaxCelsius = temperatureMax?.getOrNull(index),
            temperatureMinCelsius = temperatureMin?.getOrNull(index),
            precipitationProbability = precipitationProbabilityMax?.getOrNull(index),
            weatherCode = weatherCode?.getOrNull(index),
        )
    }

package com.eric.contentfeed.feed.data.local

import com.eric.contentfeed.core.database.WeatherCacheEntity
import com.eric.contentfeed.feed.domain.model.WeatherData
import com.eric.contentfeed.feed.domain.model.WeatherForecastDay
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

@JsonClass(generateAdapter = true)
internal data class WeatherForecastJson(
    val date: String,
    val temperatureMaxCelsius: Double?,
    val temperatureMinCelsius: Double?,
    val precipitationProbability: Int?,
    val weatherCode: Int?,
)

internal class WeatherCacheJsonCodec(
    moshi: Moshi,
) {
    private val adapter =
        moshi.adapter<List<WeatherForecastJson>>(
            Types.newParameterizedType(List::class.java, WeatherForecastJson::class.java),
        )

    fun encode(data: WeatherData): WeatherCacheEntity =
        WeatherCacheEntity(
            currentTemperatureCelsius = data.temperatureCelsius,
            currentApparentTemperatureCelsius = data.apparentTemperatureCelsius,
            currentWeatherCode = data.weatherCode,
            currentWindSpeedKmh = data.windSpeedKmh,
            forecastJson = adapter.toJson(data.forecast.map(::toJsonModel)),
        )

    fun decode(cache: WeatherCacheEntity): WeatherData =
        WeatherData(
            temperatureCelsius = cache.currentTemperatureCelsius,
            apparentTemperatureCelsius = cache.currentApparentTemperatureCelsius,
            weatherCode = cache.currentWeatherCode,
            windSpeedKmh = cache.currentWindSpeedKmh,
            forecast =
                runCatching {
                    adapter.fromJson(cache.forecastJson).orEmpty().map(::toDomainModel)
                }.getOrDefault(emptyList()),
        )

    private fun toJsonModel(day: WeatherForecastDay): WeatherForecastJson =
        WeatherForecastJson(
            date = day.date,
            temperatureMaxCelsius = day.temperatureMaxCelsius,
            temperatureMinCelsius = day.temperatureMinCelsius,
            precipitationProbability = day.precipitationProbability,
            weatherCode = day.weatherCode,
        )

    private fun toDomainModel(day: WeatherForecastJson): WeatherForecastDay =
        WeatherForecastDay(
            date = day.date,
            temperatureMaxCelsius = day.temperatureMaxCelsius,
            temperatureMinCelsius = day.temperatureMinCelsius,
            precipitationProbability = day.precipitationProbability,
            weatherCode = day.weatherCode,
        )
}

package com.eric.contentfeed.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

const val DEFAULT_WEATHER_CACHE_KEY = "default"

@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey val cacheKey: String = DEFAULT_WEATHER_CACHE_KEY,
    val currentTemperatureCelsius: Double?,
    val currentApparentTemperatureCelsius: Double?,
    val currentWeatherCode: Int?,
    val currentWindSpeedKmh: Double?,
    val forecastJson: String,
)

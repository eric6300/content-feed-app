package com.eric.contentfeed.feed.data.local

import com.eric.contentfeed.feed.domain.model.WeatherData
import com.eric.contentfeed.feed.domain.model.WeatherForecastDay
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WeatherCacheJsonCodecTest {
    private lateinit var codec: WeatherCacheJsonCodec

    @Before
    fun setUp() {
        codec = WeatherCacheJsonCodec(Moshi.Builder().build())
    }

    @Test
    fun roundTripPreservesNullableCurrentAndForecastFields() {
        val weather =
            WeatherData(
                temperatureCelsius = 23.5,
                apparentTemperatureCelsius = null,
                weatherCode = 2,
                windSpeedKmh = null,
                forecast =
                    listOf(
                        WeatherForecastDay(
                            date = "2026-09-14",
                            temperatureMaxCelsius = 27.0,
                            temperatureMinCelsius = null,
                            precipitationProbability = 60,
                            weatherCode = null,
                        ),
                    ),
            )

        val result = codec.decode(codec.encode(weather))

        assertEquals(weather, result)
    }

    @Test
    fun malformedForecastJsonBecomesAnEmptyForecast() {
        val encoded =
            codec.encode(
                WeatherData(
                    temperatureCelsius = null,
                    apparentTemperatureCelsius = null,
                    weatherCode = null,
                    windSpeedKmh = null,
                    forecast = emptyList(),
                ),
            )
        val cache = encoded.copy(forecastJson = "not-json")

        assertEquals(emptyList<WeatherForecastDay>(), codec.decode(cache).forecast)
    }
}

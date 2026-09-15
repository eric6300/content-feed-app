package com.eric.contentfeed.feed.data.remote.openmeteo

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class OpenMeteoDtoTest {
    private lateinit var moshi: Moshi

    @Before
    fun setUp() {
        moshi = Moshi.Builder().build()
    }

    @Test
    fun parsesARealForecastResponse() {
        // Captured from a live call to
        // https://api.open-meteo.com/v1/forecast?latitude=25.0330&longitude=121.5654
        //   &current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m
        //   &daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,weather_code
        //   &timezone=auto&forecast_days=5
        val json =
            """
            {
              "latitude": 25.0625,
              "longitude": 121.5625,
              "timezone": "Asia/Taipei",
              "current_units": {"temperature_2m": "°C", "wind_speed_10m": "km/h"},
              "current": {
                "time": "2026-09-15T09:00",
                "temperature_2m": 24.5,
                "apparent_temperature": 27.1,
                "weather_code": 53,
                "wind_speed_10m": 2.7
              },
              "daily": {
                "time": ["2026-09-15", "2026-09-16", "2026-09-17", "2026-09-18", "2026-09-19"],
                "temperature_2m_max": [30.1, 29.8, 31.0, 30.5, 29.9],
                "temperature_2m_min": [24.2, 24.0, 24.5, 24.1, 23.8],
                "precipitation_probability_max": [40, 60, 20, 10, null],
                "weather_code": [53, 61, 3, 1, 2]
              }
            }
            """.trimIndent()

        val dto = moshi.adapter(OpenMeteoForecastResponseDto::class.java).fromJson(json)!!

        assertEquals(24.5, dto.current?.temperature2m)
        assertEquals(27.1, dto.current?.apparentTemperature)
        assertEquals(53, dto.current?.weatherCode)
        assertEquals(2.7, dto.current?.windSpeed10m)

        assertEquals(5, dto.daily?.time?.size)
        assertEquals(listOf(30.1, 29.8, 31.0, 30.5, 29.9), dto.daily?.temperatureMax)
        assertEquals(listOf(24.2, 24.0, 24.5, 24.1, 23.8), dto.daily?.temperatureMin)
        assertEquals(listOf(40, 60, 20, 10, null), dto.daily?.precipitationProbabilityMax)
        assertEquals(listOf(53, 61, 3, 1, 2), dto.daily?.weatherCode)
    }

    @Test
    fun currentAndDailyAreIndependentlyNullable() {
        val dto = moshi.adapter(OpenMeteoForecastResponseDto::class.java).fromJson("{}")!!

        assertNull(dto.current)
        assertNull(dto.daily)
    }
}

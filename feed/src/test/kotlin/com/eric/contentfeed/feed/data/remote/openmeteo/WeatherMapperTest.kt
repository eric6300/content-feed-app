package com.eric.contentfeed.feed.data.remote.openmeteo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherMapperTest {
    @Test
    fun fullResponseMapsToWeatherDataWithAllForecastDaysInOrder() {
        val dto =
            OpenMeteoForecastResponseDto(
                current = OpenMeteoCurrentDto(24.5, 27.1, 53, 2.7),
                daily =
                    OpenMeteoDailyDto(
                        time = listOf("2026-09-15", "2026-09-16", "2026-09-17", "2026-09-18", "2026-09-19"),
                        temperatureMax = listOf(30.1, 29.8, 31.0, 30.5, 29.9),
                        temperatureMin = listOf(24.2, 24.0, 24.5, 24.1, 23.8),
                        precipitationProbabilityMax = listOf(40, 60, 20, 10, 5),
                        weatherCode = listOf(53, 61, 3, 1, 2),
                    ),
            )

        val weather = dto.toWeatherData()

        assertEquals(24.5, weather.temperatureCelsius)
        assertEquals(27.1, weather.apparentTemperatureCelsius)
        assertEquals(53, weather.weatherCode)
        assertEquals(2.7, weather.windSpeedKmh)
        assertEquals(5, weather.forecast.size)
        assertEquals("2026-09-15", weather.forecast[0].date)
        assertEquals(30.1, weather.forecast[0].temperatureMaxCelsius)
        assertEquals(61, weather.forecast[1].weatherCode)
    }

    @Test
    fun unrecognizedCurrentWeatherCodeSurvivesUnmodified() {
        val dto = OpenMeteoForecastResponseDto(current = OpenMeteoCurrentDto(20.0, 20.0, 999, 1.0), daily = null)

        assertEquals(999, dto.toWeatherData().weatherCode)
    }

    @Test
    fun unrecognizedDailyWeatherCodeSurvivesUnmodified() {
        val dto =
            OpenMeteoForecastResponseDto(
                current = null,
                daily =
                    OpenMeteoDailyDto(
                        time = listOf("2026-09-15"),
                        temperatureMax = listOf(30.0),
                        temperatureMin = listOf(24.0),
                        precipitationProbabilityMax = listOf(10),
                        weatherCode = listOf(999),
                    ),
            )

        assertEquals(
            999,
            dto
                .toWeatherData()
                .forecast
                .single()
                .weatherCode,
        )
    }

    @Test
    fun nullElementWithinADailyArrayMapsToNullOnThatDay() {
        val dto =
            OpenMeteoForecastResponseDto(
                current = null,
                daily =
                    OpenMeteoDailyDto(
                        time = listOf("2026-09-15"),
                        temperatureMax = listOf(30.0),
                        temperatureMin = listOf(24.0),
                        precipitationProbabilityMax = listOf(null),
                        weatherCode = listOf(53),
                    ),
            )

        assertNull(
            dto
                .toWeatherData()
                .forecast
                .single()
                .precipitationProbability,
        )
    }

    @Test
    fun absentDailyBecomesAnEmptyForecast() {
        val dto = OpenMeteoForecastResponseDto(current = OpenMeteoCurrentDto(20.0, 20.0, 1, 1.0), daily = null)

        assertTrue(dto.toWeatherData().forecast.isEmpty())
    }

    @Test
    fun absentCurrentBecomesAllNullCurrentFields() {
        val dto = OpenMeteoForecastResponseDto(current = null, daily = null)

        val weather = dto.toWeatherData()
        assertNull(weather.temperatureCelsius)
        assertNull(weather.apparentTemperatureCelsius)
        assertNull(weather.weatherCode)
        assertNull(weather.windSpeedKmh)
    }

    @Test
    fun aShorterSecondaryArrayThanTimeMapsToNullRatherThanCrashing() {
        val dto =
            OpenMeteoForecastResponseDto(
                current = null,
                daily =
                    OpenMeteoDailyDto(
                        time = listOf("2026-09-15", "2026-09-16"),
                        temperatureMax = listOf(30.0),
                        temperatureMin = listOf(24.0, 23.0),
                        precipitationProbabilityMax = listOf(10, 20),
                        weatherCode = listOf(53, 61),
                    ),
            )

        val forecast = dto.toWeatherData().forecast
        assertEquals(30.0, forecast[0].temperatureMaxCelsius)
        assertNull(forecast[1].temperatureMaxCelsius)
    }
}

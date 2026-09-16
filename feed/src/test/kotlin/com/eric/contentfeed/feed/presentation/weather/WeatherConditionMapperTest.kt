package com.eric.contentfeed.feed.presentation.weather

import com.eric.contentfeed.feed.presentation.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherConditionMapperTest {
    @Test
    fun mapsRepresentativeWmoRangesToPresentationConditions() {
        assertEquals(WeatherCondition.ClearSky, WeatherConditionMapper.map(0))
        assertEquals(WeatherCondition.PartlyCloudy, WeatherConditionMapper.map(2))
        assertEquals(WeatherCondition.Fog, WeatherConditionMapper.map(48))
        assertEquals(WeatherCondition.Drizzle, WeatherConditionMapper.map(56))
        assertEquals(WeatherCondition.Rain, WeatherConditionMapper.map(82))
        assertEquals(WeatherCondition.Snow, WeatherConditionMapper.map(86))
        assertEquals(WeatherCondition.Thunderstorm, WeatherConditionMapper.map(99))
    }

    @Test
    fun nullAndUnrecognisedCodesRemainExplicitlyUnknown() {
        assertEquals(WeatherCondition.Unknown, WeatherConditionMapper.map(null))
        assertEquals(WeatherCondition.Unknown, WeatherConditionMapper.map(100))
    }
}

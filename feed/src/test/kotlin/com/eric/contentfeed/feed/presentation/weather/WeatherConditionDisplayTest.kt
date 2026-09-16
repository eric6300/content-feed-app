package com.eric.contentfeed.feed.presentation.weather

import com.eric.contentfeed.feed.R
import com.eric.contentfeed.feed.presentation.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherConditionDisplayTest {
    @Test
    fun bindsKnownConditionToIconAndLabel() {
        assertEquals(
            WeatherConditionDisplay(WeatherConditionIcon.Clear, R.string.weather_condition_clear),
            WeatherCondition.ClearSky.display(),
        )
        assertEquals(
            WeatherConditionDisplay(WeatherConditionIcon.Rain, R.string.weather_condition_rain),
            WeatherCondition.Rain.display(),
        )
    }

    @Test
    fun bindsUnknownConditionToNeutralIconAndLabel() {
        assertEquals(
            WeatherConditionDisplay(WeatherConditionIcon.Unknown, R.string.weather_condition_unknown),
            WeatherCondition.Unknown.display(),
        )
    }
}

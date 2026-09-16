package com.eric.contentfeed.feed.presentation.weather

import com.eric.contentfeed.feed.presentation.model.WeatherCondition

/** Maps the WMO code table into a small stable vocabulary for presentation. */
object WeatherConditionMapper {
    fun map(code: Int?): WeatherCondition =
        when (code) {
            0 -> WeatherCondition.ClearSky
            1, 2 -> WeatherCondition.PartlyCloudy
            3 -> WeatherCondition.Overcast
            45, 48 -> WeatherCondition.Fog
            in 51..57 -> WeatherCondition.Drizzle
            in 61..67, in 80..82 -> WeatherCondition.Rain
            in 71..77, in 85..86 -> WeatherCondition.Snow
            in 95..99 ->
                when (code) {
                    95, 96, 99 -> WeatherCondition.Thunderstorm
                    else -> WeatherCondition.Unknown
                }
            else -> WeatherCondition.Unknown
        }
}

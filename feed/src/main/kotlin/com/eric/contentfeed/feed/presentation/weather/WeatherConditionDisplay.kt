package com.eric.contentfeed.feed.presentation.weather

import androidx.annotation.StringRes
import com.eric.contentfeed.feed.R
import com.eric.contentfeed.feed.presentation.model.WeatherCondition

enum class WeatherConditionIcon {
    Clear,
    PartlyCloudy,
    Cloud,
    Fog,
    Rain,
    Snow,
    Thunderstorm,
    Unknown,
}

data class WeatherConditionDisplay(
    val icon: WeatherConditionIcon,
    @StringRes val labelRes: Int,
)

fun WeatherCondition.display(): WeatherConditionDisplay =
    when (this) {
        WeatherCondition.ClearSky ->
            WeatherConditionDisplay(WeatherConditionIcon.Clear, R.string.weather_condition_clear)
        WeatherCondition.PartlyCloudy ->
            WeatherConditionDisplay(
                WeatherConditionIcon.PartlyCloudy,
                R.string.weather_condition_partly_cloudy,
            )
        WeatherCondition.Overcast ->
            WeatherConditionDisplay(WeatherConditionIcon.Cloud, R.string.weather_condition_overcast)
        WeatherCondition.Fog -> WeatherConditionDisplay(WeatherConditionIcon.Fog, R.string.weather_condition_fog)
        WeatherCondition.Drizzle ->
            WeatherConditionDisplay(WeatherConditionIcon.Rain, R.string.weather_condition_drizzle)
        WeatherCondition.Rain ->
            WeatherConditionDisplay(WeatherConditionIcon.Rain, R.string.weather_condition_rain)
        WeatherCondition.Snow -> WeatherConditionDisplay(WeatherConditionIcon.Snow, R.string.weather_condition_snow)
        WeatherCondition.Thunderstorm ->
            WeatherConditionDisplay(
                WeatherConditionIcon.Thunderstorm,
                R.string.weather_condition_thunderstorm,
            )
        WeatherCondition.Unknown ->
            WeatherConditionDisplay(WeatherConditionIcon.Unknown, R.string.weather_condition_unknown)
    }

package com.eric.contentfeed.feed.data.remote

internal const val SPACEFLIGHT_NEWS_BASE_URL = "https://api.spaceflightnewsapi.net/v4/"
internal const val OPEN_METEO_BASE_URL = "https://api.open-meteo.com/v1/"

// Each endpoint's path lives next to the base URL it's relative to, one place per
// source; the API interfaces reference these constants directly in @GET(...).
internal const val SPACEFLIGHT_NEWS_ARTICLES_PATH = "articles/"
internal const val OPEN_METEO_FORECAST_PATH = "forecast"

// TODO(pending product decision if this ever needs to change): fixed display location.
internal const val WEATHER_LATITUDE = 25.0330
internal const val WEATHER_LONGITUDE = 121.5654

/** "auto" resolves the IANA zone from the coordinates above, so the timezone can never
 * contradict the location constants. */
internal const val WEATHER_TIMEZONE = "auto"
internal const val WEATHER_FORECAST_DAYS = 5

internal const val WEATHER_CURRENT_FIELDS =
    "temperature_2m,apparent_temperature,weather_code,wind_speed_10m"
internal const val WEATHER_DAILY_FIELDS =
    "temperature_2m_max,temperature_2m_min,precipitation_probability_max,weather_code"

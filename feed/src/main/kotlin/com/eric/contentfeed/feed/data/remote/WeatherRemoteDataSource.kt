package com.eric.contentfeed.feed.data.remote

import com.eric.contentfeed.feed.domain.model.WeatherData

interface WeatherRemoteDataSource {
    suspend fun fetchCurrentAndForecast(): RemoteResult<WeatherData>
}

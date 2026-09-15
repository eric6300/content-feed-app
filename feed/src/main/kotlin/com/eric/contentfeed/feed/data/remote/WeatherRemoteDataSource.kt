package com.eric.contentfeed.feed.data.remote

import com.eric.contentfeed.feed.domain.model.RemoteFailure
import com.eric.contentfeed.feed.domain.model.WeatherData

interface WeatherRemoteDataSource {
    suspend fun fetchCurrentAndForecast(): WeatherFetchResult
}

sealed interface WeatherFetchResult {
    data class Loaded(
        val weather: WeatherData,
    ) : WeatherFetchResult

    data class Failure(
        val cause: RemoteFailure,
    ) : WeatherFetchResult
}

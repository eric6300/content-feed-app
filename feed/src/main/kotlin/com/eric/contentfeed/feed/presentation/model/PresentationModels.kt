package com.eric.contentfeed.feed.presentation.model

import androidx.compose.runtime.Immutable
import com.eric.contentfeed.feed.domain.model.RemoteFailure

enum class WeatherCondition {
    ClearSky,
    PartlyCloudy,
    Overcast,
    Fog,
    Drizzle,
    Rain,
    Snow,
    Thunderstorm,
    Unknown,
}

@Immutable
data class ArticleUiModel(
    val id: Int,
    val title: String,
    val source: String,
    val authors: List<String>,
    val summary: String?,
    val imageUrl: String?,
    val articleUrl: String,
    val publishedAtEpochMillis: Long?,
    val isSaved: Boolean,
    val localImagePath: String?,
)

@Immutable
data class WeatherForecastUiModel(
    val date: String,
    val temperatureMaxCelsius: Double?,
    val temperatureMinCelsius: Double?,
    val precipitationProbability: Int?,
    val condition: WeatherCondition,
)

@Immutable
data class WeatherUiModel(
    val temperatureCelsius: Double?,
    val apparentTemperatureCelsius: Double?,
    val condition: WeatherCondition,
    val windSpeedKmh: Double?,
    val forecast: List<WeatherForecastUiModel>,
)

@Immutable
data class ServiceCardUiModel(
    val id: Int,
    val title: String,
    val description: String,
    val blurb: String,
    val price: Double?,
    val imageAssetPath: String,
    val targetUrl: String,
    val poolIndex: Int,
    val assignmentSequence: Long,
)

sealed interface FeedItemUiModel {
    data class Article(
        val value: ArticleUiModel,
    ) : FeedItemUiModel

    data class ServiceCard(
        val value: ServiceCardUiModel,
    ) : FeedItemUiModel
}

sealed interface DetailTarget {
    data class Article(
        val articleId: Int,
    ) : DetailTarget

    data class ServiceCard(
        val poolIndex: Int,
        val assignmentSequence: Long,
    ) : DetailTarget
}

sealed interface DetailUiModel {
    data class Article(
        val value: ArticleUiModel,
    ) : DetailUiModel

    data class ServiceCard(
        val value: ServiceCardUiModel,
    ) : DetailUiModel
}

data class SourceUiError(
    val cause: RemoteFailure,
)

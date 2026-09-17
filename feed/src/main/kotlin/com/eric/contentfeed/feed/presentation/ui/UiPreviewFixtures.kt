package com.eric.contentfeed.feed.presentation.ui

import com.eric.contentfeed.core.connectivity.ConnectivityStatus
import com.eric.contentfeed.feed.presentation.contract.DetailContract
import com.eric.contentfeed.feed.presentation.contract.FeedContract
import com.eric.contentfeed.feed.presentation.contract.SavedContract
import com.eric.contentfeed.feed.presentation.model.ArticleUiModel
import com.eric.contentfeed.feed.presentation.model.DetailUiModel
import com.eric.contentfeed.feed.presentation.model.FeedItemUiModel
import com.eric.contentfeed.feed.presentation.model.ServiceCardUiModel
import com.eric.contentfeed.feed.presentation.model.WeatherCondition
import com.eric.contentfeed.feed.presentation.model.WeatherForecastUiModel
import com.eric.contentfeed.feed.presentation.model.WeatherUiModel
import java.time.Instant

internal object UiPreviewFixtures {
    private val publishedAt = Instant.parse("2025-04-18T09:00:00Z").toEpochMilli()

    val article =
        ArticleUiModel(
            id = 101,
            title = "A new view of Earth arrives from orbit",
            source = "Spaceflight Now",
            authors = listOf("Morgan Lee"),
            summary = "A compact summary with enough detail to preview the article row and its saved state.",
            imageUrl = null,
            articleUrl = "https://example.com/articles/earth-view",
            publishedAtEpochMillis = publishedAt,
            isSaved = false,
            localImagePath = null,
        )

    val savedArticle =
        article.copy(
            id = 102,
            title = "The next generation of small launch vehicles",
            source = "Orbital Today",
            isSaved = true,
        )

    val serviceCard =
        ServiceCardUiModel(
            id = 1,
            title = "Stargazing essentials",
            description =
                "A curated collection of practical tools for planning a clear night under the stars.",
            blurb = "Gear and guides selected for your next night sky session.",
            price = 48.5,
            imageAssetPath = "service-cards/service-001.jpg",
            targetUrl = "https://example.com/services/stargazing",
            poolIndex = 0,
            assignmentSequence = 1L,
        )

    val weather =
        WeatherUiModel(
            temperatureCelsius = 22.0,
            apparentTemperatureCelsius = 23.0,
            condition = WeatherCondition.PartlyCloudy,
            windSpeedKmh = 11.0,
            forecast =
                listOf(
                    WeatherForecastUiModel(
                        date = "Today",
                        temperatureMaxCelsius = 24.0,
                        temperatureMinCelsius = 18.0,
                        precipitationProbability = 10,
                        condition = WeatherCondition.PartlyCloudy,
                    ),
                    WeatherForecastUiModel(
                        date = "Tomorrow",
                        temperatureMaxCelsius = 25.0,
                        temperatureMinCelsius = 19.0,
                        precipitationProbability = 20,
                        condition = WeatherCondition.ClearSky,
                    ),
                ),
        )

    val feedState =
        FeedContract.State(
            weather = FeedContract.WeatherState.Content(value = weather, isRefreshing = false, error = null),
            articles =
                FeedContract.ArticleStreamState.Content(
                    items =
                        listOf(
                            FeedItemUiModel.Article(article),
                            FeedItemUiModel.ServiceCard(serviceCard),
                            FeedItemUiModel.Article(savedArticle),
                        ),
                    isRefreshing = false,
                    error = null,
                    pagination = FeedContract.PaginationState.End,
                ),
            connectivityStatus = ConnectivityStatus.Online,
        )

    val savedEmptyState = SavedContract.State(SavedContract.ContentState.Empty)

    val savedReadyState = SavedContract.State(SavedContract.ContentState.Ready(listOf(article, savedArticle)))

    val articleDetailState =
        DetailContract.State(
            content = DetailContract.ContentState.Ready(DetailUiModel.Article(article)),
        )

    val serviceDetailState =
        DetailContract.State(
            content = DetailContract.ContentState.Ready(DetailUiModel.ServiceCard(serviceCard)),
        )
}

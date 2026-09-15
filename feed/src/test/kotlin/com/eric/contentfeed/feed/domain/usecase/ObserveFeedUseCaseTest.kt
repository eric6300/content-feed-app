package com.eric.contentfeed.feed.domain.usecase

import app.cash.turbine.test
import com.eric.contentfeed.feed.data.local.FixedServiceCardCatalog
import com.eric.contentfeed.feed.domain.model.Article
import com.eric.contentfeed.feed.domain.model.CachedArticle
import com.eric.contentfeed.feed.domain.model.FeedItem
import com.eric.contentfeed.feed.domain.model.FeedPlacement
import com.eric.contentfeed.feed.domain.model.RemoteFailure
import com.eric.contentfeed.feed.domain.model.SERVICE_CARD_CONTENT_TYPE
import com.eric.contentfeed.feed.domain.model.SourceStatus
import com.eric.contentfeed.feed.domain.model.WeatherData
import com.eric.contentfeed.feed.repository.ArticleRepository
import com.eric.contentfeed.feed.repository.WeatherRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveFeedUseCaseTest {
    private val article =
        Article(
            id = 1,
            title = "Article 1",
            source = "Source",
            authors = emptyList(),
            summary = null,
            imageUrl = null,
            articleUrl = "https://example.com/1",
            publishedAtEpochMillis = 100L,
        )
    private val cachedArticle =
        CachedArticle(
            article = article,
            isSaved = false,
            savedAtEpochMillis = null,
            localImagePath = null,
            pendingUnsaveAtEpochMillis = null,
        )
    private val weather =
        WeatherData(
            temperatureCelsius = 20.0,
            apparentTemperatureCelsius = 19.0,
            weatherCode = 1,
            windSpeedKmh = 3.0,
            forecast = emptyList(),
        )

    private fun useCase(
        articles: List<CachedArticle>,
        placements: List<FeedPlacement>,
        refreshStatus: SourceStatus,
        appendStatus: SourceStatus,
        isLastPage: Boolean,
        weatherValue: WeatherData?,
        weatherStatus: SourceStatus,
    ): ObserveFeedUseCase {
        val articleRepository =
            mockk<ArticleRepository> {
                every { observeArticles() } returns flowOf(articles)
                every { observePlacements() } returns flowOf(placements)
                every { this@mockk.refreshStatus } returns MutableStateFlow(refreshStatus)
                every { this@mockk.appendStatus } returns MutableStateFlow(appendStatus)
                every { this@mockk.isLastPage } returns MutableStateFlow(isLastPage)
            }
        val weatherRepository =
            mockk<WeatherRepository> {
                every { observeWeather() } returns flowOf(weatherValue)
                every { status } returns MutableStateFlow(weatherStatus)
            }
        return ObserveFeedUseCase(articleRepository, weatherRepository, FixedServiceCardCatalog(emptyList()))
    }

    @Test
    fun combinesArticlesPlacementsAndWeatherIntoOneSnapshot() =
        runTest {
            val placement =
                FeedPlacement(
                    anchorArticleId = 1,
                    anchorPublishedAtEpochMillis = 100L,
                    contentType = SERVICE_CARD_CONTENT_TYPE,
                    poolIndex = 0,
                    assignmentSequence = 0L,
                )
            val useCase =
                useCase(
                    articles = listOf(cachedArticle),
                    placements = listOf(placement),
                    refreshStatus = SourceStatus.Ready,
                    appendStatus = SourceStatus.Idle,
                    isLastPage = false,
                    weatherValue = weather,
                    weatherStatus = SourceStatus.Ready,
                )

            useCase().test {
                val snapshot = awaitItem()
                assertEquals(weather, snapshot.weather)
                assertEquals(SourceStatus.Ready, snapshot.weatherStatus)
                assertEquals(listOf(FeedItem.ArticleItem(cachedArticle)), snapshot.items)
                assertEquals(SourceStatus.Ready, snapshot.articleStreamStatus.refresh)
                assertEquals(false, snapshot.articleStreamStatus.isLastPage)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun emptySuccessfulPageIsDistinctFromAFailureWithAnEmptyCache() =
        runTest {
            val successCase =
                useCase(
                    articles = emptyList(),
                    placements = emptyList(),
                    refreshStatus = SourceStatus.Ready,
                    appendStatus = SourceStatus.Idle,
                    isLastPage = false,
                    weatherValue = null,
                    weatherStatus = SourceStatus.Ready,
                )
            val failureCase =
                useCase(
                    articles = emptyList(),
                    placements = emptyList(),
                    refreshStatus = SourceStatus.Failed(RemoteFailure.NetworkUnavailable),
                    appendStatus = SourceStatus.Idle,
                    isLastPage = false,
                    weatherValue = null,
                    weatherStatus = SourceStatus.Failed(RemoteFailure.NetworkUnavailable),
                )

            successCase().test {
                val snapshot = awaitItem()
                assertEquals(emptyList<FeedItem>(), snapshot.items)
                assertEquals(SourceStatus.Ready, snapshot.articleStreamStatus.refresh)
                cancelAndIgnoreRemainingEvents()
            }
            failureCase().test {
                val snapshot = awaitItem()
                // Same empty item list as the success case, but distinguishable
                // through status alone — no separate boolean flag needed.
                assertEquals(emptyList<FeedItem>(), snapshot.items)
                assertEquals(
                    SourceStatus.Failed(RemoteFailure.NetworkUnavailable),
                    snapshot.articleStreamStatus.refresh,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
}

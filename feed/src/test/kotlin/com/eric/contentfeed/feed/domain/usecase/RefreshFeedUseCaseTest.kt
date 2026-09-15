package com.eric.contentfeed.feed.domain.usecase

import com.eric.contentfeed.feed.domain.model.RemoteFailure
import com.eric.contentfeed.feed.repository.ArticleRepository
import com.eric.contentfeed.feed.repository.RefreshOutcome
import com.eric.contentfeed.feed.repository.WeatherRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class RefreshFeedUseCaseTest {
    private lateinit var articleRepository: ArticleRepository
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var useCase: RefreshFeedUseCase

    @Before
    fun setUp() {
        articleRepository = mockk(relaxed = true)
        weatherRepository = mockk(relaxed = true)
        useCase = RefreshFeedUseCase(articleRepository, weatherRepository)
    }

    @Test
    fun aFailingWeatherSourceDoesNotBlockOrRollBackArticles() =
        runTest {
            coEvery { weatherRepository.refresh(any()) } returns RefreshOutcome.Failed(RemoteFailure.NetworkUnavailable)
            coEvery { articleRepository.refreshTop(any()) } returns RefreshOutcome.Succeeded

            useCase(RefreshTrigger.Manual)

            coVerify { weatherRepository.refresh(true) }
            coVerify { articleRepository.refreshTop(true) }
        }

    @Test
    fun aFailingArticleSourceDoesNotBlockWeather() =
        runTest {
            coEvery { weatherRepository.refresh(any()) } returns RefreshOutcome.Succeeded
            coEvery { articleRepository.refreshTop(any()) } returns RefreshOutcome.Failed(RemoteFailure.Http(500))

            useCase(RefreshTrigger.InitialOpen)

            coVerify { weatherRepository.refresh(false) }
            coVerify { articleRepository.refreshTop(false) }
        }

    @Test
    fun manualTriggerBypassesFreshnessForBothSourcesAndNeverRunsCleanup() =
        runTest {
            coEvery { weatherRepository.refresh(any()) } returns RefreshOutcome.Succeeded
            coEvery { articleRepository.refreshTop(any()) } returns RefreshOutcome.Succeeded

            useCase(RefreshTrigger.Manual)

            coVerify { weatherRepository.refresh(true) }
            coVerify { articleRepository.refreshTop(true) }
            coVerify(exactly = 0) { articleRepository.pruneStaleUnsavedArticles() }
        }

    @Test
    fun reconnectTriggerRespectsFreshnessAndNeverRunsCleanup() =
        runTest {
            coEvery { weatherRepository.refresh(any()) } returns RefreshOutcome.Succeeded
            coEvery { articleRepository.refreshTop(any()) } returns RefreshOutcome.Succeeded

            useCase(RefreshTrigger.Reconnect)

            coVerify { weatherRepository.refresh(false) }
            coVerify { articleRepository.refreshTop(false) }
            coVerify(exactly = 0) { articleRepository.pruneStaleUnsavedArticles() }
        }

    @Test
    fun initialOpenRunsCleanupOnlyWhenTheArticleRefreshActuallySucceeded() =
        runTest {
            coEvery { weatherRepository.refresh(any()) } returns RefreshOutcome.Succeeded
            coEvery { articleRepository.refreshTop(any()) } returns RefreshOutcome.Succeeded

            useCase(RefreshTrigger.InitialOpen)

            coVerify(exactly = 1) { articleRepository.pruneStaleUnsavedArticles() }
        }

    @Test
    fun initialOpenSkipsCleanupWhenTheRefreshWasSkippedAsAlreadyFresh() =
        runTest {
            coEvery { weatherRepository.refresh(any()) } returns RefreshOutcome.Skipped
            coEvery { articleRepository.refreshTop(any()) } returns RefreshOutcome.Skipped

            useCase(RefreshTrigger.InitialOpen)

            coVerify(exactly = 0) { articleRepository.pruneStaleUnsavedArticles() }
        }

    @Test
    fun initialOpenSkipsCleanupWhenTheRefreshFailed() =
        runTest {
            coEvery { weatherRepository.refresh(any()) } returns RefreshOutcome.Succeeded
            coEvery { articleRepository.refreshTop(any()) } returns RefreshOutcome.Failed(RemoteFailure.Unknown)

            useCase(RefreshTrigger.InitialOpen)

            coVerify(exactly = 0) { articleRepository.pruneStaleUnsavedArticles() }
        }
}

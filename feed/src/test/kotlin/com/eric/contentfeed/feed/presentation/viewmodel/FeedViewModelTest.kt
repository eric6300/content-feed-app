package com.eric.contentfeed.feed.presentation.viewmodel

import com.eric.contentfeed.core.connectivity.ConnectivityStatus
import com.eric.contentfeed.feed.domain.model.ArticleStreamStatus
import com.eric.contentfeed.feed.domain.model.FeedRefreshResult
import com.eric.contentfeed.feed.domain.model.FeedSnapshot
import com.eric.contentfeed.feed.domain.model.RemoteFailure
import com.eric.contentfeed.feed.domain.model.SourceRefreshResult
import com.eric.contentfeed.feed.domain.model.SourceStatus
import com.eric.contentfeed.feed.domain.usecase.ObserveConnectivityUseCase
import com.eric.contentfeed.feed.domain.usecase.ObserveFeedUseCase
import com.eric.contentfeed.feed.domain.usecase.RefreshFeedUseCase
import com.eric.contentfeed.feed.domain.usecase.RefreshTrigger
import com.eric.contentfeed.feed.domain.usecase.SaveArticleUseCase
import com.eric.contentfeed.feed.domain.usecase.UnsaveArticleUseCase
import com.eric.contentfeed.feed.presentation.contract.FeedContract
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var connectivity: MutableStateFlow<ConnectivityStatus>
    private lateinit var viewModel: FeedViewModel
    private val refreshFeed = mockk<RefreshFeedUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        connectivity = MutableStateFlow(ConnectivityStatus.Online)

        val observeFeed = mockk<ObserveFeedUseCase>()
        val observeConnectivity = mockk<ObserveConnectivityUseCase>()
        every { observeFeed() } returns MutableStateFlow(emptySnapshot())
        every { observeConnectivity() } returns connectivity
        coEvery { refreshFeed(RefreshTrigger.Reconnect) } returns skippedRefresh()

        viewModel =
            FeedViewModel(
                observeFeed = observeFeed,
                refreshFeed = refreshFeed,
                loadNextArticlePage = mockk(),
                saveArticle = mockk<SaveArticleUseCase>(),
                unsaveArticle = mockk<UnsaveArticleUseCase>(),
                observeConnectivity = observeConnectivity,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun reconnectTriggersARefreshThatRespectsFreshness() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            connectivity.value = ConnectivityStatus.Offline
            advanceUntilIdle()
            connectivity.value = ConnectivityStatus.Online
            advanceUntilIdle()

            coVerify(exactly = 1) { refreshFeed(RefreshTrigger.Reconnect) }
        }

    @Test
    fun manualRefreshFailureEmitsAScopedSourceError() =
        runTest(testDispatcher) {
            coEvery { refreshFeed(RefreshTrigger.Manual) } returns
                FeedRefreshResult(
                    articles = SourceRefreshResult.Failed(RemoteFailure.Http(503)),
                    weather = SourceRefreshResult.Skipped,
                )
            val effect = async { viewModel.effects.first() }
            runCurrent()

            viewModel.onEvent(FeedContract.Event.Refresh)
            advanceUntilIdle()

            assertEquals(
                FeedContract.Effect.SourceRefreshFailed(
                    source = FeedContract.Source.Articles,
                    cause = RemoteFailure.Http(503),
                ),
                effect.await(),
            )
        }

    private fun emptySnapshot() =
        FeedSnapshot(
            weather = null,
            weatherStatus = SourceStatus.Ready,
            items = emptyList(),
            articleStreamStatus =
                ArticleStreamStatus(
                    refresh = SourceStatus.Ready,
                    append = SourceStatus.Idle,
                    isLastPage = false,
                ),
        )

    private fun skippedRefresh() =
        FeedRefreshResult(
            articles = SourceRefreshResult.Skipped,
            weather = SourceRefreshResult.Skipped,
        )
}

package com.eric.contentfeed.feed.presentation.viewmodel

import app.cash.turbine.test
import com.eric.contentfeed.core.connectivity.ConnectivityStatus
import com.eric.contentfeed.feed.domain.model.Article
import com.eric.contentfeed.feed.domain.model.CachedArticle
import com.eric.contentfeed.feed.domain.usecase.ObserveArticleUseCase
import com.eric.contentfeed.feed.domain.usecase.ObserveConnectivityUseCase
import com.eric.contentfeed.feed.domain.usecase.ResolveServiceCardUseCase
import com.eric.contentfeed.feed.domain.usecase.SaveArticleUseCase
import com.eric.contentfeed.feed.domain.usecase.UnsaveArticleUseCase
import com.eric.contentfeed.feed.domain.usecase.UnsaveSource
import com.eric.contentfeed.feed.presentation.contract.DetailContract
import com.eric.contentfeed.feed.presentation.model.DetailTarget
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val saveArticle = mockk<SaveArticleUseCase>(relaxed = true)
    private val unsaveArticle = mockk<UnsaveArticleUseCase>(relaxed = true)
    private val resolveServiceCard = mockk<ResolveServiceCardUseCase>()
    private lateinit var connectivity: MutableStateFlow<ConnectivityStatus>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        connectivity = MutableStateFlow(ConnectivityStatus.Online)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModelFor(article: CachedArticle?): DetailViewModel = viewModelForContent(flowOf(article))

    private fun viewModelForLoading(): DetailViewModel = viewModelForContent(emptyFlow())

    private fun viewModelForContent(articleContent: Flow<CachedArticle?>): DetailViewModel {
        val observeArticle = mockk<ObserveArticleUseCase>()
        every { observeArticle(1) } returns articleContent
        val observeConnectivity = mockk<ObserveConnectivityUseCase>()
        every { observeConnectivity() } returns connectivity
        return DetailViewModel(
            target = DetailTarget.Article(1),
            observeArticle = observeArticle,
            resolveServiceCard = resolveServiceCard,
            saveArticle = saveArticle,
            unsaveArticle = unsaveArticle,
            observeConnectivity = observeConnectivity,
        )
    }

    @Test
    fun openExternalLinkEmitsOpenExternalUrlWhenOnline() =
        runTest(testDispatcher) {
            connectivity.value = ConnectivityStatus.Online
            val viewModel = viewModelFor(cachedArticle(1))
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onEvent(DetailContract.Event.OpenExternalLink)
                assertEquals(
                    DetailContract.Effect.OpenExternalUrl("https://example.com/articles/1"),
                    awaitItem(),
                )
            }
        }

    @Test
    fun openExternalLinkEmitsUnavailableWhenOffline() =
        runTest(testDispatcher) {
            connectivity.value = ConnectivityStatus.Offline
            val viewModel = viewModelFor(cachedArticle(1))
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onEvent(DetailContract.Event.OpenExternalLink)
                assertEquals(DetailContract.Effect.ExternalLinkUnavailable, awaitItem())
            }
        }

    @Test
    fun openExternalLinkEmitsUnavailableWhenConnectivityIsUnknown() =
        runTest(testDispatcher) {
            connectivity.value = ConnectivityStatus.Unknown
            val viewModel = viewModelFor(cachedArticle(1))
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onEvent(DetailContract.Event.OpenExternalLink)
                assertEquals(DetailContract.Effect.ExternalLinkUnavailable, awaitItem())
            }
        }

    @Test
    fun openExternalLinkEmitsNothingWhenContentIsNotFound() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor(null)
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onEvent(DetailContract.Event.OpenExternalLink)
                expectNoEvents()
            }
        }

    @Test
    fun openExternalLinkEmitsNothingWhileContentIsLoading() =
        runTest(testDispatcher) {
            val viewModel = viewModelForLoading()
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onEvent(DetailContract.Event.OpenExternalLink)
                expectNoEvents()
            }
        }

    @Test
    fun toggleSaveUnsavesAnAlreadySavedArticle() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor(cachedArticle(1, isSaved = true))
            advanceUntilIdle()

            viewModel.onEvent(DetailContract.Event.ToggleSave)
            advanceUntilIdle()

            coVerify(exactly = 1) { unsaveArticle(1, UnsaveSource.FeedOrDetail) }
            coVerify(exactly = 0) { saveArticle(any()) }
        }

    @Test
    fun toggleSaveSavesAnUnsavedArticle() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor(cachedArticle(1, isSaved = false))
            advanceUntilIdle()

            viewModel.onEvent(DetailContract.Event.ToggleSave)
            advanceUntilIdle()

            coVerify(exactly = 1) { saveArticle(1) }
            coVerify(exactly = 0) { unsaveArticle(any(), any()) }
        }

    private fun cachedArticle(
        id: Int,
        isSaved: Boolean = true,
    ) = CachedArticle(
        article =
            Article(
                id = id,
                title = "Article $id",
                source = "Source",
                authors = emptyList(),
                summary = "Summary",
                imageUrl = null,
                articleUrl = "https://example.com/articles/$id",
                publishedAtEpochMillis = 1_000L,
            ),
        isSaved = isSaved,
        savedAtEpochMillis = 1_000L,
        localImagePath = null,
        pendingUnsaveAtEpochMillis = null,
    )
}

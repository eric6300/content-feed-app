package com.eric.contentfeed.feed.presentation.viewmodel

import com.eric.contentfeed.feed.domain.model.Article
import com.eric.contentfeed.feed.domain.model.CachedArticle
import com.eric.contentfeed.feed.domain.usecase.FinalizePendingUnsavesUseCase
import com.eric.contentfeed.feed.domain.usecase.FinalizeTrigger
import com.eric.contentfeed.feed.domain.usecase.ObserveSavedArticlesUseCase
import com.eric.contentfeed.feed.domain.usecase.UndoUnsaveArticleUseCase
import com.eric.contentfeed.feed.domain.usecase.UnsaveArticleUseCase
import com.eric.contentfeed.feed.domain.usecase.UnsaveSource
import com.eric.contentfeed.feed.presentation.contract.SavedContract
import com.eric.contentfeed.feed.repository.FeedPolicy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SavedViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SavedViewModel
    private val unsaveArticle = mockk<UnsaveArticleUseCase>()
    private val undoUnsaveArticle = mockk<UndoUnsaveArticleUseCase>(relaxed = true)
    private val finalizePendingUnsaves = mockk<FinalizePendingUnsavesUseCase>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val observeSavedArticles = mockk<ObserveSavedArticlesUseCase>()
        every { observeSavedArticles() } returns flowOf(listOf(cachedArticle(1)))
        coEvery { unsaveArticle(any(), UnsaveSource.SavedList) } returns true
        viewModel =
            SavedViewModel(
                observeSavedArticles = observeSavedArticles,
                unsaveArticle = unsaveArticle,
                undoUnsaveArticle = undoUnsaveArticle,
                finalizePendingUnsaves = finalizePendingUnsaves,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun pendingRemovalsFinalizeIndependently() =
        runTest(testDispatcher) {
            advanceUntilIdle()
            removeAndAwaitUndo(1)
            advanceTimeBy(2.seconds)
            removeAndAwaitUndo(2)

            advanceTimeBy(3.seconds)
            runCurrent()
            coVerify(exactly = 1) {
                finalizePendingUnsaves(FinalizeTrigger.UndoWindowElapsed(1))
            }
            coVerify(exactly = 0) {
                finalizePendingUnsaves(FinalizeTrigger.UndoWindowElapsed(2))
            }

            advanceTimeBy(2.seconds)
            runCurrent()
            coVerify(exactly = 1) {
                finalizePendingUnsaves(FinalizeTrigger.UndoWindowElapsed(2))
            }
        }

    @Test
    fun undoCancelsOnlyThatArticleTimer() =
        runTest(testDispatcher) {
            advanceUntilIdle()
            removeAndAwaitUndo(1)
            viewModel.onEvent(SavedContract.Event.UndoRemoval(1))
            runCurrent()

            advanceTimeBy(FeedPolicy.UNDO_WINDOW + 1.seconds)
            runCurrent()

            coVerify(exactly = 1) { undoUnsaveArticle(1) }
            coVerify(exactly = 0) {
                finalizePendingUnsaves(FinalizeTrigger.UndoWindowElapsed(1))
            }
        }

    @Test
    fun undoRemovalEmitsUndoUnavailableWhenTheRemovalWasAlreadyFinalized() =
        runTest(testDispatcher) {
            advanceUntilIdle()
            removeAndAwaitUndo(1)
            coEvery { undoUnsaveArticle(1) } returns false

            val effect = async { viewModel.effects.first() }
            viewModel.onEvent(SavedContract.Event.UndoRemoval(1))
            runCurrent()

            assertEquals(SavedContract.Effect.UndoUnavailable, effect.await())
        }

    private suspend fun TestScope.removeAndAwaitUndo(articleId: Int) {
        val effect = async { viewModel.effects.first() }
        viewModel.onEvent(SavedContract.Event.RemoveArticle(articleId))
        runCurrent()
        assertEquals(SavedContract.Effect.ShowUndo(articleId), effect.await())
    }

    private fun cachedArticle(id: Int) =
        CachedArticle(
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
            isSaved = true,
            savedAtEpochMillis = 1_000L,
            localImagePath = null,
            pendingUnsaveAtEpochMillis = null,
        )
}

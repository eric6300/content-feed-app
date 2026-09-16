package com.eric.contentfeed.feed.domain.usecase

import app.cash.turbine.test
import com.eric.contentfeed.feed.domain.model.CachedArticle
import com.eric.contentfeed.feed.repository.SavedArticleRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveSavedArticlesUseCaseTest {
    @Test
    fun emptySavedListIsEmitted() =
        runTest {
            val repository =
                mockk<SavedArticleRepository> {
                    every { observeSavedArticles() } returns flowOf(emptyList())
                }

            ObserveSavedArticlesUseCase(repository)().test {
                assertEquals(emptyList<CachedArticle>(), awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun populatedSavedListWithAndWithoutImagePathsIsEmitted() =
        runTest {
            val saved = listOf(mockk<CachedArticle>(), mockk<CachedArticle>())
            val repository =
                mockk<SavedArticleRepository> {
                    every { observeSavedArticles() } returns flowOf(saved)
                }

            ObserveSavedArticlesUseCase(repository)().test {
                assertEquals(saved, awaitItem())
                awaitComplete()
            }
        }
}

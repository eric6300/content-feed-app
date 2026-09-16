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

class ObserveArticleUseCaseTest {
    @Test
    fun savedDetailEmitsArticleAndThenNullWhenRoomDoes() =
        runTest {
            val article = mockk<CachedArticle>()
            val repository =
                mockk<SavedArticleRepository> {
                    every { observeArticle(42) } returns flowOf(article, null)
                }

            ObserveArticleUseCase(repository)(42).test {
                assertEquals(article, awaitItem())
                assertEquals(null, awaitItem())
                awaitComplete()
            }
        }
}

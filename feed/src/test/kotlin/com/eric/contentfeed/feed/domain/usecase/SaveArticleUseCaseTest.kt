package com.eric.contentfeed.feed.domain.usecase

import com.eric.contentfeed.feed.repository.SavedArticleRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SaveArticleUseCaseTest {
    @Test
    fun saveUsesTheSharedCommandPathForEveryArticleEntryPoint() =
        runTest {
            val repository = mockk<SavedArticleRepository>(relaxed = true)
            val useCase = SaveArticleUseCase(repository)

            useCase(42)

            // Feed, detail, and Saved are T6 call sites of this identical command;
            // the use-case layer intentionally does not duplicate screen-shaped tests.
            coVerify(exactly = 1) { repository.saveArticle(42) }
        }
}

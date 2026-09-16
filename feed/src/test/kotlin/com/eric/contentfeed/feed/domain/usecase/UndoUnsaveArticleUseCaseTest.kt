package com.eric.contentfeed.feed.domain.usecase

import com.eric.contentfeed.feed.repository.SavedArticleRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UndoUnsaveArticleUseCaseTest {
    @Test
    fun undoDelegatesToTheSavedArticleRepository() =
        runTest {
            val repository = mockk<SavedArticleRepository>(relaxed = true)

            UndoUnsaveArticleUseCase(repository)(42)

            coVerify(exactly = 1) { repository.undoRemoval(42) }
        }
}

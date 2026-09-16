package com.eric.contentfeed.feed.domain.usecase

import com.eric.contentfeed.feed.repository.SavedArticleRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UnsaveArticleUseCaseTest {
    @Test
    fun feedOrDetailUsesImmediateRemoval() =
        runTest {
            val repository = mockk<SavedArticleRepository>(relaxed = true)

            UnsaveArticleUseCase(repository)(42, UnsaveSource.FeedOrDetail)

            coVerify(exactly = 1) { repository.unsaveArticleImmediately(42) }
        }

    @Test
    fun savedListUsesPendingRemoval() =
        runTest {
            val repository = mockk<SavedArticleRepository>(relaxed = true)

            UnsaveArticleUseCase(repository)(42, UnsaveSource.SavedList)

            coVerify(exactly = 1) { repository.removeFromSavedList(42) }
        }
}

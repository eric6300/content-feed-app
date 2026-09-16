package com.eric.contentfeed.feed.domain.usecase

import com.eric.contentfeed.feed.repository.SavedArticleRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FinalizePendingUnsavesUseCaseTest {
    @Test
    fun appStartFinalizesEveryPendingRemoval() =
        runTest {
            val repository = mockk<SavedArticleRepository>(relaxed = true)

            FinalizePendingUnsavesUseCase(repository)(FinalizeTrigger.AppStart)

            coVerify(exactly = 1) { repository.finalizeAllPendingRemovals() }
        }

    @Test
    fun elapsedWindowFinalizesOnlyExpiredRemovals() =
        runTest {
            val repository = mockk<SavedArticleRepository>(relaxed = true)

            FinalizePendingUnsavesUseCase(repository)(FinalizeTrigger.UndoWindowElapsed)

            coVerify(exactly = 1) { repository.finalizeExpiredRemovals() }
        }
}

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
    fun dismissedWindowFinalizesOnlyTheDismissedArticle() =
        runTest {
            val repository = mockk<SavedArticleRepository>(relaxed = true)

            FinalizePendingUnsavesUseCase(repository)(FinalizeTrigger.UndoWindowElapsed(articleId = 42))

            coVerify(exactly = 1) { repository.finalizeRemoval(42) }
        }

    @Test
    fun foregroundReturnOnlyFinalizesRemovalsPastTheirDeadline() =
        runTest {
            val repository = mockk<SavedArticleRepository>(relaxed = true)

            FinalizePendingUnsavesUseCase(repository)(FinalizeTrigger.ForegroundReturn)

            coVerify(exactly = 1) { repository.finalizeExpiredRemovals() }
            coVerify(exactly = 0) { repository.finalizeAllPendingRemovals() }
        }
}

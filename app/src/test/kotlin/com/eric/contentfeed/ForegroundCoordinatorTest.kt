package com.eric.contentfeed

import com.eric.contentfeed.feed.domain.model.FeedRefreshResult
import com.eric.contentfeed.feed.domain.model.RemoteFailure
import com.eric.contentfeed.feed.domain.model.SourceRefreshResult
import com.eric.contentfeed.feed.domain.usecase.FinalizePendingUnsavesUseCase
import com.eric.contentfeed.feed.domain.usecase.FinalizeTrigger
import com.eric.contentfeed.feed.domain.usecase.RefreshFeedUseCase
import com.eric.contentfeed.feed.domain.usecase.RefreshTrigger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ForegroundCoordinatorTest {
    @Test
    fun coldStartFinalizesPendingRemovalsBeforeRefreshingSources() =
        runTest {
            val finalize = mockk<FinalizePendingUnsavesUseCase>()
            val refresh = mockk<RefreshFeedUseCase>()
            coEvery { finalize(FinalizeTrigger.AppStart) } returns Unit
            coEvery { refresh(RefreshTrigger.InitialOpen) } returns
                FeedRefreshResult(SourceRefreshResult.Skipped, SourceRefreshResult.Skipped)

            ForegroundCoordinator(finalize, refresh).onForeground()

            coVerifyOrder {
                finalize(FinalizeTrigger.AppStart)
                refresh(RefreshTrigger.InitialOpen)
            }
        }

    @Test
    fun coldStartRefreshFailuresArePublishedAsScopedEffects() =
        runTest {
            val finalize = mockk<FinalizePendingUnsavesUseCase>()
            val refresh = mockk<RefreshFeedUseCase>()
            coEvery { finalize(FinalizeTrigger.AppStart) } returns Unit
            coEvery { refresh(RefreshTrigger.InitialOpen) } returns
                FeedRefreshResult(
                    articles = SourceRefreshResult.Failed(RemoteFailure.Http(502)),
                    weather = SourceRefreshResult.Skipped,
                )
            val coordinator = ForegroundCoordinator(finalize, refresh)
            val effect = async { coordinator.effects.first() }

            coordinator.onForeground()

            assertEquals(
                ForegroundEffect.SourceRefreshFailed(
                    source = ForegroundEffect.Source.Articles,
                    cause = RemoteFailure.Http(502),
                ),
                effect.await(),
            )
        }

    @Test
    fun secondForegroundReturnUsesTheDeadlineRespectingTriggersNotColdStart() =
        runTest {
            val finalize = mockk<FinalizePendingUnsavesUseCase>()
            val refresh = mockk<RefreshFeedUseCase>()
            coEvery { finalize(any()) } returns Unit
            coEvery { refresh(any()) } returns
                FeedRefreshResult(SourceRefreshResult.Skipped, SourceRefreshResult.Skipped)
            val coordinator = ForegroundCoordinator(finalize, refresh)

            coordinator.onForeground()
            coordinator.onForeground()

            coVerifyOrder {
                finalize(FinalizeTrigger.AppStart)
                refresh(RefreshTrigger.InitialOpen)
                finalize(FinalizeTrigger.ForegroundReturn)
                refresh(RefreshTrigger.ForegroundReturn)
            }
            coVerify(exactly = 1) { finalize(FinalizeTrigger.AppStart) }
            coVerify(exactly = 1) { refresh(RefreshTrigger.InitialOpen) }
        }
}

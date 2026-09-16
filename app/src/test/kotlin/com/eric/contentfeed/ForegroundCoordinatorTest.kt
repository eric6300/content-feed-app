package com.eric.contentfeed

import com.eric.contentfeed.feed.domain.model.FeedRefreshResult
import com.eric.contentfeed.feed.domain.model.RemoteFailure
import com.eric.contentfeed.feed.domain.model.SourceRefreshResult
import com.eric.contentfeed.feed.domain.usecase.FinalizePendingUnsavesUseCase
import com.eric.contentfeed.feed.domain.usecase.FinalizeTrigger
import com.eric.contentfeed.feed.domain.usecase.RefreshFeedUseCase
import com.eric.contentfeed.feed.domain.usecase.RefreshTrigger
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ForegroundCoordinatorTest {
    @Test
    fun appStartFinalizesPendingRemovalsBeforeRefreshingSources() =
        runTest {
            val finalize = mockk<FinalizePendingUnsavesUseCase>()
            val refresh = mockk<RefreshFeedUseCase>()
            coEvery { finalize(FinalizeTrigger.AppStart) } returns Unit
            coEvery { refresh(RefreshTrigger.InitialOpen) } returns
                FeedRefreshResult(SourceRefreshResult.Skipped, SourceRefreshResult.Skipped)

            ForegroundCoordinator(finalize, refresh).onStart()

            coVerifyOrder {
                finalize(FinalizeTrigger.AppStart)
                refresh(RefreshTrigger.InitialOpen)
            }
        }

    @Test
    fun initialRefreshFailuresArePublishedAsScopedEffects() =
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

            coordinator.onStart()

            assertEquals(
                ForegroundEffect.SourceRefreshFailed(
                    source = ForegroundEffect.Source.Articles,
                    cause = RemoteFailure.Http(502),
                ),
                effect.await(),
            )
        }
}

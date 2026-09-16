package com.eric.contentfeed

import com.eric.contentfeed.feed.domain.model.FeedRefreshResult
import com.eric.contentfeed.feed.domain.model.RemoteFailure
import com.eric.contentfeed.feed.domain.model.SourceRefreshResult
import com.eric.contentfeed.feed.domain.usecase.FinalizePendingUnsavesUseCase
import com.eric.contentfeed.feed.domain.usecase.FinalizeTrigger
import com.eric.contentfeed.feed.domain.usecase.RefreshFeedUseCase
import com.eric.contentfeed.feed.domain.usecase.RefreshTrigger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface ForegroundEffect {
    data class SourceRefreshFailed(
        val source: Source,
        val cause: RemoteFailure,
    ) : ForegroundEffect

    enum class Source {
        Articles,
        Weather,
    }
}

/** Coordinates the two app-start responsibilities that must happen in order. */
class ForegroundCoordinator(
    private val finalizePendingUnsaves: FinalizePendingUnsavesUseCase,
    private val refreshFeed: RefreshFeedUseCase,
) {
    private val startMutex = Mutex()
    private val effectChannel = Channel<ForegroundEffect>(Channel.BUFFERED)

    val effects: Flow<ForegroundEffect> = effectChannel.receiveAsFlow()

    suspend fun onStart() {
        startMutex.withLock {
            finalizePendingUnsaves(FinalizeTrigger.AppStart)
            refreshFeed(RefreshTrigger.InitialOpen).emitFailures()
        }
    }

    private suspend fun FeedRefreshResult.emitFailures() {
        val articleResult = articles
        if (articleResult is SourceRefreshResult.Failed) {
            effectChannel.send(
                ForegroundEffect.SourceRefreshFailed(
                    source = ForegroundEffect.Source.Articles,
                    cause = articleResult.cause,
                ),
            )
        }
        val weatherResult = weather
        if (weatherResult is SourceRefreshResult.Failed) {
            effectChannel.send(
                ForegroundEffect.SourceRefreshFailed(
                    source = ForegroundEffect.Source.Weather,
                    cause = weatherResult.cause,
                ),
            )
        }
    }
}

package com.eric.contentfeed.feed.domain.usecase

import com.eric.contentfeed.core.connectivity.ConnectivityObserver
import com.eric.contentfeed.core.connectivity.ConnectivityStatus
import kotlinx.coroutines.flow.StateFlow

/** Exposes the shared process connectivity signal without leaking the Android API. */
class ObserveConnectivityUseCase(
    private val observer: ConnectivityObserver,
) {
    operator fun invoke(): StateFlow<ConnectivityStatus> = observer.status
}

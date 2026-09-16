package com.eric.contentfeed.feed.presentation.contract

import androidx.compose.runtime.Immutable
import com.eric.contentfeed.core.connectivity.ConnectivityStatus

interface ConnectivityContract {
    @Immutable
    data class State(
        val status: ConnectivityStatus = ConnectivityStatus.Unknown,
    )

    sealed interface Event {
        data class Changed(
            val status: ConnectivityStatus,
        ) : Event
    }

    sealed interface Effect {
        data object BackOnline : Effect
    }
}

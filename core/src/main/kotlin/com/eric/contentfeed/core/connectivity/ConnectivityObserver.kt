package com.eric.contentfeed.core.connectivity

import kotlinx.coroutines.flow.StateFlow

/** The connectivity states that matter to the app's user-facing policy. */
sealed interface ConnectivityStatus {
    data object Unknown : ConnectivityStatus

    data object Online : ConnectivityStatus

    data object Offline : ConnectivityStatus
}

/** Process-wide connectivity source shared by the app shell and feature ViewModels. */
interface ConnectivityObserver {
    val status: StateFlow<ConnectivityStatus>
}

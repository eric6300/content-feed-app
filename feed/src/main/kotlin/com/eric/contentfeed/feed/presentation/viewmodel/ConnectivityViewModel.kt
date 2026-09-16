package com.eric.contentfeed.feed.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.eric.contentfeed.core.connectivity.ConnectivityStatus
import com.eric.contentfeed.feed.domain.usecase.ObserveConnectivityUseCase
import com.eric.contentfeed.feed.presentation.contract.ConnectivityContract
import com.eric.contentfeed.feed.presentation.mvi.MviViewModel
import kotlinx.coroutines.launch

class ConnectivityViewModel(
    observeConnectivity: ObserveConnectivityUseCase,
) : MviViewModel<ConnectivityContract.State, ConnectivityContract.Event, ConnectivityContract.Effect>(
        initialState = ConnectivityContract.State(),
    ) {
    private var previousStatus: ConnectivityStatus = ConnectivityStatus.Unknown

    init {
        viewModelScope.launch {
            observeConnectivity().collect(::onConnectivityChanged)
        }
    }

    override suspend fun handleEvent(event: ConnectivityContract.Event) {
        when (event) {
            is ConnectivityContract.Event.Changed -> onConnectivityChanged(event.status)
        }
    }

    private suspend fun onConnectivityChanged(status: ConnectivityStatus) {
        updateState { ConnectivityContract.State(status) }
        if (previousStatus == ConnectivityStatus.Offline && status == ConnectivityStatus.Online) {
            emitEffect(ConnectivityContract.Effect.BackOnline)
        }
        previousStatus = status
    }
}

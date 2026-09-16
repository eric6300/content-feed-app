package com.eric.contentfeed.feed.presentation.contract

import androidx.compose.runtime.Immutable
import com.eric.contentfeed.feed.presentation.model.DetailUiModel

interface DetailContract {
    @Immutable
    data class State(
        val content: ContentState = ContentState.Loading,
        val isOffline: Boolean = false,
    )

    sealed interface Event {
        data object ToggleSave : Event

        data object OpenExternalLink : Event
    }

    sealed interface Effect {
        data class OpenExternalUrl(
            val url: String,
        ) : Effect

        data object ExternalLinkUnavailable : Effect
    }

    sealed interface ContentState {
        data object Loading : ContentState

        data object NotFound : ContentState

        data class Ready(
            val value: DetailUiModel,
        ) : ContentState
    }
}

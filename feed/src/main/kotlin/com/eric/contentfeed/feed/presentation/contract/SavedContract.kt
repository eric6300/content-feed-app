package com.eric.contentfeed.feed.presentation.contract

import androidx.compose.runtime.Immutable
import com.eric.contentfeed.feed.presentation.model.ArticleUiModel

interface SavedContract {
    @Immutable
    data class State(
        val content: ContentState = ContentState.Loading,
    )

    sealed interface Event {
        data class OpenArticle(
            val articleId: Int,
        ) : Event

        data class RemoveArticle(
            val articleId: Int,
        ) : Event

        data class UndoRemoval(
            val articleId: Int,
        ) : Event

        data class UndoWindowElapsed(
            val articleId: Int,
        ) : Event
    }

    sealed interface Effect {
        data class NavigateToArticle(
            val articleId: Int,
        ) : Effect

        data class ShowUndo(
            val articleId: Int,
        ) : Effect
    }

    sealed interface ContentState {
        data object Loading : ContentState

        data object Empty : ContentState

        data class Ready(
            val items: List<ArticleUiModel>,
        ) : ContentState
    }
}

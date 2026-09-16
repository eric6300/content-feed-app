package com.eric.contentfeed.feed.presentation.contract

import androidx.compose.runtime.Immutable
import com.eric.contentfeed.core.connectivity.ConnectivityStatus
import com.eric.contentfeed.feed.domain.model.RemoteFailure
import com.eric.contentfeed.feed.presentation.model.FeedItemUiModel
import com.eric.contentfeed.feed.presentation.model.WeatherUiModel

interface FeedContract {
    @Immutable
    data class State(
        val weather: WeatherState = WeatherState.Loading,
        val articles: ArticleStreamState = ArticleStreamState.Loading,
        val connectivityStatus: ConnectivityStatus = ConnectivityStatus.Unknown,
    )

    sealed interface Event {
        data object Refresh : Event

        data object LoadNextPage : Event

        data object RetryNextPage : Event

        data class ToggleSave(
            val articleId: Int,
        ) : Event

        data class OpenArticle(
            val articleId: Int,
        ) : Event

        data class OpenServiceCard(
            val poolIndex: Int,
            val assignmentSequence: Long,
        ) : Event

        data class OpenExternalLink(
            val url: String,
        ) : Event
    }

    sealed interface Effect {
        data class NavigateToArticle(
            val articleId: Int,
        ) : Effect

        data class NavigateToServiceCard(
            val poolIndex: Int,
            val assignmentSequence: Long,
        ) : Effect

        data class OpenExternalUrl(
            val url: String,
        ) : Effect

        data object ExternalLinkUnavailable : Effect

        data class SourceRefreshFailed(
            val source: Source,
            val cause: RemoteFailure,
        ) : Effect
    }

    enum class Source {
        Articles,
        Weather,
    }

    sealed interface WeatherState {
        data object Loading : WeatherState

        data object Empty : WeatherState

        data class Error(
            val cause: RemoteFailure,
        ) : WeatherState

        data class Content(
            val value: WeatherUiModel,
            val isRefreshing: Boolean,
            val error: RemoteFailure?,
        ) : WeatherState
    }

    sealed interface ArticleStreamState {
        data object Loading : ArticleStreamState

        data object Empty : ArticleStreamState

        data object OfflineEmpty : ArticleStreamState

        data class Error(
            val cause: RemoteFailure,
        ) : ArticleStreamState

        data class Content(
            val items: List<FeedItemUiModel>,
            val isRefreshing: Boolean,
            val error: RemoteFailure?,
            val pagination: PaginationState,
        ) : ArticleStreamState
    }

    sealed interface PaginationState {
        data object Idle : PaginationState

        data object Loading : PaginationState

        data class RetryableError(
            val cause: RemoteFailure,
        ) : PaginationState

        data object End : PaginationState
    }

    companion object {
        fun isOffline(status: ConnectivityStatus): Boolean = status == ConnectivityStatus.Offline
    }
}

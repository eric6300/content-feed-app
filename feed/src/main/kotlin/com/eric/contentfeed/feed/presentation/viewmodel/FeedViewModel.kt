package com.eric.contentfeed.feed.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.eric.contentfeed.core.connectivity.ConnectivityStatus
import com.eric.contentfeed.feed.domain.model.ArticleStreamStatus
import com.eric.contentfeed.feed.domain.model.FeedItem
import com.eric.contentfeed.feed.domain.model.FeedRefreshResult
import com.eric.contentfeed.feed.domain.model.FeedSnapshot
import com.eric.contentfeed.feed.domain.model.RemoteFailure
import com.eric.contentfeed.feed.domain.model.SourceRefreshResult
import com.eric.contentfeed.feed.domain.model.SourceStatus
import com.eric.contentfeed.feed.domain.model.WeatherData
import com.eric.contentfeed.feed.domain.usecase.LoadNextArticlePageUseCase
import com.eric.contentfeed.feed.domain.usecase.ObserveConnectivityUseCase
import com.eric.contentfeed.feed.domain.usecase.ObserveFeedUseCase
import com.eric.contentfeed.feed.domain.usecase.RefreshFeedUseCase
import com.eric.contentfeed.feed.domain.usecase.RefreshTrigger
import com.eric.contentfeed.feed.domain.usecase.SaveArticleUseCase
import com.eric.contentfeed.feed.domain.usecase.UnsaveArticleUseCase
import com.eric.contentfeed.feed.domain.usecase.UnsaveSource
import com.eric.contentfeed.feed.presentation.contract.FeedContract
import com.eric.contentfeed.feed.presentation.model.ArticleUiModel
import com.eric.contentfeed.feed.presentation.model.FeedItemUiModel
import com.eric.contentfeed.feed.presentation.model.ServiceCardUiModel
import com.eric.contentfeed.feed.presentation.model.WeatherForecastUiModel
import com.eric.contentfeed.feed.presentation.model.WeatherUiModel
import com.eric.contentfeed.feed.presentation.model.toArticleUiModel
import com.eric.contentfeed.feed.presentation.mvi.MviViewModel
import com.eric.contentfeed.feed.presentation.weather.WeatherConditionMapper
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class FeedViewModel(
    observeFeed: ObserveFeedUseCase,
    private val refreshFeed: RefreshFeedUseCase,
    private val loadNextArticlePage: LoadNextArticlePageUseCase,
    private val saveArticle: SaveArticleUseCase,
    private val unsaveArticle: UnsaveArticleUseCase,
    observeConnectivity: ObserveConnectivityUseCase,
) : MviViewModel<FeedContract.State, FeedContract.Event, FeedContract.Effect>(
        initialState = FeedContract.State(),
    ) {
    private var connectivityStatus: ConnectivityStatus = ConnectivityStatus.Unknown
    private var previousConnectivity: ConnectivityStatus = ConnectivityStatus.Unknown

    init {
        viewModelScope.launch {
            combine(observeFeed(), observeConnectivity()) { snapshot, connectivity ->
                snapshot to connectivity
            }.collect { (snapshot, connectivity) ->
                val reconnected =
                    previousConnectivity == ConnectivityStatus.Offline &&
                        connectivity == ConnectivityStatus.Online
                previousConnectivity = connectivity
                connectivityStatus = connectivity
                updateState { snapshot.toPresentationState(connectivity) }
                if (reconnected) {
                    launch { refreshForReconnect() }
                }
            }
        }
    }

    override suspend fun handleEvent(event: FeedContract.Event) {
        when (event) {
            FeedContract.Event.Refresh -> refreshManually()
            FeedContract.Event.LoadNextPage,
            FeedContract.Event.RetryNextPage,
            -> loadNextPage()
            is FeedContract.Event.ToggleSave -> toggleSave(event.articleId)
            is FeedContract.Event.OpenArticle -> emitEffect(FeedContract.Effect.NavigateToArticle(event.articleId))
            is FeedContract.Event.OpenServiceCard ->
                emitEffect(
                    FeedContract.Effect.NavigateToServiceCard(
                        poolIndex = event.poolIndex,
                        assignmentSequence = event.assignmentSequence,
                    ),
                )
            is FeedContract.Event.OpenExternalLink -> openExternalLink(event.url)
        }
    }

    private suspend fun refreshManually() {
        val result = refreshFeed(RefreshTrigger.Manual)
        result.emitFailures()
    }

    private suspend fun refreshForReconnect() {
        val result = refreshFeed(RefreshTrigger.Reconnect)
        result.emitFailures()
    }

    private suspend fun FeedRefreshResult.emitFailures() {
        if (articles is SourceRefreshResult.Failed) {
            emitEffect(
                FeedContract.Effect.SourceRefreshFailed(
                    source = FeedContract.Source.Articles,
                    cause = articles.cause,
                ),
            )
        }
        if (weather is SourceRefreshResult.Failed) {
            emitEffect(
                FeedContract.Effect.SourceRefreshFailed(
                    source = FeedContract.Source.Weather,
                    cause = weather.cause,
                ),
            )
        }
    }

    private suspend fun loadNextPage() {
        val content = state.value.articles as? FeedContract.ArticleStreamState.Content ?: return
        if (content.pagination is FeedContract.PaginationState.Loading ||
            content.pagination is FeedContract.PaginationState.End
        ) {
            return
        }
        loadNextArticlePage()
    }

    private suspend fun toggleSave(articleId: Int) {
        val article = findArticle(articleId) ?: return
        if (article.isSaved) {
            unsaveArticle(articleId, UnsaveSource.FeedOrDetail)
        } else {
            saveArticle(articleId)
        }
    }

    private suspend fun openExternalLink(url: String) {
        if (connectivityStatus == ConnectivityStatus.Online) {
            emitEffect(FeedContract.Effect.OpenExternalUrl(url))
        } else {
            emitEffect(FeedContract.Effect.ExternalLinkUnavailable)
        }
    }

    private fun findArticle(articleId: Int): ArticleUiModel? =
        (state.value.articles as? FeedContract.ArticleStreamState.Content)
            ?.items
            ?.filterIsInstance<FeedItemUiModel.Article>()
            ?.firstOrNull { it.value.id == articleId }
            ?.value
}

private fun FeedSnapshot.toPresentationState(connectivity: ConnectivityStatus): FeedContract.State =
    FeedContract.State(
        weather = weather.toWeatherState(weatherStatus),
        articles = items.toArticleStreamState(articleStreamStatus, connectivity),
        connectivityStatus = connectivity,
    )

private fun WeatherData?.toWeatherState(status: SourceStatus): FeedContract.WeatherState {
    if (this == null) {
        return when (status) {
            SourceStatus.Idle,
            SourceStatus.Loading,
            -> FeedContract.WeatherState.Loading
            SourceStatus.Ready -> FeedContract.WeatherState.Empty
            is SourceStatus.Failed -> FeedContract.WeatherState.Error(status.cause)
        }
    }

    return FeedContract.WeatherState.Content(
        value = toUiModel(),
        isRefreshing = status == SourceStatus.Loading,
        error = (status as? SourceStatus.Failed)?.cause,
    )
}

private fun WeatherData.toUiModel(): WeatherUiModel =
    WeatherUiModel(
        temperatureCelsius = temperatureCelsius,
        apparentTemperatureCelsius = apparentTemperatureCelsius,
        condition = WeatherConditionMapper.map(weatherCode),
        windSpeedKmh = windSpeedKmh,
        forecast =
            forecast.map { day ->
                WeatherForecastUiModel(
                    date = day.date,
                    temperatureMaxCelsius = day.temperatureMaxCelsius,
                    temperatureMinCelsius = day.temperatureMinCelsius,
                    precipitationProbability = day.precipitationProbability,
                    condition = WeatherConditionMapper.map(day.weatherCode),
                )
            },
    )

private fun RemoteFailure.explainedByOffline(connectivity: ConnectivityStatus): Boolean =
    this == RemoteFailure.NetworkUnavailable && connectivity == ConnectivityStatus.Offline

private fun List<FeedItem>.toArticleStreamState(
    status: ArticleStreamStatus,
    connectivity: ConnectivityStatus,
): FeedContract.ArticleStreamState {
    if (isEmpty()) {
        return when (val refresh = status.refresh) {
            SourceStatus.Idle,
            SourceStatus.Loading,
            -> FeedContract.ArticleStreamState.Loading
            SourceStatus.Ready -> FeedContract.ArticleStreamState.Empty
            is SourceStatus.Failed ->
                if (refresh.cause.explainedByOffline(connectivity)) {
                    FeedContract.ArticleStreamState.OfflineEmpty
                } else {
                    FeedContract.ArticleStreamState.Error(refresh.cause)
                }
        }
    }

    return FeedContract.ArticleStreamState.Content(
        items = map(FeedItem::toUiModel),
        isRefreshing = status.refresh == SourceStatus.Loading,
        error =
            (status.refresh as? SourceStatus.Failed)?.cause?.takeUnless {
                it.explainedByOffline(connectivity)
            },
        pagination = status.toPaginationState(),
    )
}

private fun ArticleStreamStatus.toPaginationState(): FeedContract.PaginationState =
    when {
        append == SourceStatus.Loading -> FeedContract.PaginationState.Loading
        append is SourceStatus.Failed -> FeedContract.PaginationState.RetryableError(append.cause)
        isLastPage -> FeedContract.PaginationState.End
        else -> FeedContract.PaginationState.Idle
    }

private fun FeedItem.toUiModel(): FeedItemUiModel =
    when (this) {
        is FeedItem.ArticleItem -> FeedItemUiModel.Article(article.toArticleUiModel())
        is FeedItem.ServiceCardItem ->
            FeedItemUiModel.ServiceCard(
                ServiceCardUiModel(
                    id = card.id,
                    title = card.title,
                    description = card.description,
                    blurb = card.blurb,
                    price = card.price,
                    imageAssetPath = card.imageAssetPath,
                    targetUrl = card.targetUrl,
                    poolIndex = placement.poolIndex,
                    assignmentSequence = placement.assignmentSequence,
                ),
            )
    }

package com.eric.contentfeed.feed.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.eric.contentfeed.core.connectivity.ConnectivityStatus
import com.eric.contentfeed.feed.domain.model.CachedArticle
import com.eric.contentfeed.feed.domain.usecase.ObserveArticleUseCase
import com.eric.contentfeed.feed.domain.usecase.ObserveConnectivityUseCase
import com.eric.contentfeed.feed.domain.usecase.ResolveServiceCardUseCase
import com.eric.contentfeed.feed.domain.usecase.SaveArticleUseCase
import com.eric.contentfeed.feed.domain.usecase.UnsaveArticleUseCase
import com.eric.contentfeed.feed.domain.usecase.UnsaveSource
import com.eric.contentfeed.feed.presentation.contract.DetailContract
import com.eric.contentfeed.feed.presentation.model.DetailTarget
import com.eric.contentfeed.feed.presentation.model.DetailUiModel
import com.eric.contentfeed.feed.presentation.model.ServiceCardUiModel
import com.eric.contentfeed.feed.presentation.model.toArticleUiModel
import com.eric.contentfeed.feed.presentation.mvi.MviViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DetailViewModel(
    private val target: DetailTarget,
    observeArticle: ObserveArticleUseCase,
    resolveServiceCard: ResolveServiceCardUseCase,
    private val saveArticle: SaveArticleUseCase,
    private val unsaveArticle: UnsaveArticleUseCase,
    observeConnectivity: ObserveConnectivityUseCase,
) : MviViewModel<DetailContract.State, DetailContract.Event, DetailContract.Effect>(
        initialState = DetailContract.State(),
    ) {
    private var connectivityStatus: ConnectivityStatus = ConnectivityStatus.Unknown

    init {
        val content: Flow<DetailContract.ContentState> =
            when (target) {
                is DetailTarget.Article ->
                    observeArticle(target.articleId).mapToDetailState()
                is DetailTarget.ServiceCard ->
                    flowOf(
                        resolveServiceCard(target.poolIndex)
                            ?.let { card ->
                                DetailContract.ContentState.Ready(
                                    DetailUiModel.ServiceCard(
                                        ServiceCardUiModel(
                                            id = card.id,
                                            title = card.title,
                                            description = card.description,
                                            blurb = card.blurb,
                                            price = card.price,
                                            imageAssetPath = card.imageAssetPath,
                                            targetUrl = card.targetUrl,
                                            poolIndex = target.poolIndex,
                                            assignmentSequence = target.assignmentSequence,
                                        ),
                                    ),
                                )
                            }
                            ?: DetailContract.ContentState.NotFound,
                    )
            }

        viewModelScope.launch {
            combine(content, observeConnectivity()) { detailState, connectivity ->
                connectivityStatus = connectivity
                DetailContract.State(
                    content = detailState,
                    isOffline = connectivity == ConnectivityStatus.Offline,
                )
            }.collect { newState -> updateState { newState } }
        }
    }

    override suspend fun handleEvent(event: DetailContract.Event) {
        when (event) {
            DetailContract.Event.ToggleSave -> toggleSave()
            DetailContract.Event.OpenExternalLink -> openExternalLink()
        }
    }

    private suspend fun toggleSave() {
        val article =
            ((state.value.content as? DetailContract.ContentState.Ready)?.value as? DetailUiModel.Article)
                ?: return
        if (article.value.isSaved) {
            unsaveArticle(article.value.id, UnsaveSource.FeedOrDetail)
        } else {
            saveArticle(article.value.id)
        }
    }

    private suspend fun openExternalLink() {
        val url =
            when (val content = state.value.content) {
                is DetailContract.ContentState.Ready -> content.value.externalUrl
                DetailContract.ContentState.Loading,
                DetailContract.ContentState.NotFound,
                -> return
            }
        if (connectivityStatus == ConnectivityStatus.Online) {
            emitEffect(DetailContract.Effect.OpenExternalUrl(url))
        } else {
            emitEffect(DetailContract.Effect.ExternalLinkUnavailable)
        }
    }
}

private fun Flow<CachedArticle?>.mapToDetailState(): Flow<DetailContract.ContentState> =
    map { article ->
        article?.let {
            DetailContract.ContentState.Ready(DetailUiModel.Article(it.toArticleUiModel()))
        } ?: DetailContract.ContentState.NotFound
    }

private val DetailUiModel.externalUrl: String
    get() =
        when (this) {
            is DetailUiModel.Article -> value.articleUrl
            is DetailUiModel.ServiceCard -> value.targetUrl
        }

package com.eric.contentfeed.feed.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.eric.contentfeed.feed.domain.model.CachedArticle
import com.eric.contentfeed.feed.domain.usecase.FinalizePendingUnsavesUseCase
import com.eric.contentfeed.feed.domain.usecase.FinalizeTrigger
import com.eric.contentfeed.feed.domain.usecase.ObserveSavedArticlesUseCase
import com.eric.contentfeed.feed.domain.usecase.UndoUnsaveArticleUseCase
import com.eric.contentfeed.feed.domain.usecase.UnsaveArticleUseCase
import com.eric.contentfeed.feed.domain.usecase.UnsaveSource
import com.eric.contentfeed.feed.presentation.contract.SavedContract
import com.eric.contentfeed.feed.presentation.model.ArticleUiModel
import com.eric.contentfeed.feed.presentation.mvi.MviViewModel
import com.eric.contentfeed.feed.repository.FeedPolicy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SavedViewModel(
    observeSavedArticles: ObserveSavedArticlesUseCase,
    private val unsaveArticle: UnsaveArticleUseCase,
    private val undoUnsaveArticle: UndoUnsaveArticleUseCase,
    private val finalizePendingUnsaves: FinalizePendingUnsavesUseCase,
) : MviViewModel<SavedContract.State, SavedContract.Event, SavedContract.Effect>(
        initialState = SavedContract.State(),
    ) {
    private val pendingRemovalJobs = mutableMapOf<Int, Job>()

    init {
        viewModelScope.launch {
            observeSavedArticles().collect { articles ->
                updateState { SavedContract.State(articles.toContentState()) }
            }
        }
    }

    override suspend fun handleEvent(event: SavedContract.Event) {
        when (event) {
            is SavedContract.Event.OpenArticle ->
                emitEffect(SavedContract.Effect.NavigateToArticle(event.articleId))
            is SavedContract.Event.RemoveArticle -> removeArticle(event.articleId)
            is SavedContract.Event.UndoRemoval -> undoRemoval(event.articleId)
            is SavedContract.Event.UndoWindowElapsed -> {
                pendingRemovalJobs.remove(event.articleId)?.cancel()
                finalizePendingUnsaves(FinalizeTrigger.UndoWindowElapsed(event.articleId))
            }
        }
    }

    private suspend fun removeArticle(articleId: Int) {
        if (unsaveArticle(articleId, UnsaveSource.SavedList)) {
            pendingRemovalJobs.remove(articleId)?.cancel()
            pendingRemovalJobs[articleId] =
                viewModelScope.launch {
                    delay(FeedPolicy.UNDO_WINDOW)
                    pendingRemovalJobs.remove(articleId)
                    finalizePendingUnsaves(FinalizeTrigger.UndoWindowElapsed(articleId))
                }
            emitEffect(SavedContract.Effect.ShowUndo(articleId))
        }
    }

    private suspend fun undoRemoval(articleId: Int) {
        pendingRemovalJobs.remove(articleId)?.cancel()
        undoUnsaveArticle(articleId)
    }
}

private fun List<CachedArticle>.toContentState(): SavedContract.ContentState =
    if (isEmpty()) {
        SavedContract.ContentState.Empty
    } else {
        SavedContract.ContentState.Ready(map(CachedArticle::toArticleUiModel))
    }

private fun CachedArticle.toArticleUiModel(): ArticleUiModel =
    ArticleUiModel(
        id = article.id,
        title = article.title,
        source = article.source,
        authors = article.authors,
        summary = article.summary,
        imageUrl = article.imageUrl,
        articleUrl = article.articleUrl,
        publishedAtEpochMillis = article.publishedAtEpochMillis,
        isSaved = isSaved,
        localImagePath = localImagePath,
    )

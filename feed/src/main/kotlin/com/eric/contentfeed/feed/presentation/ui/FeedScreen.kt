@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.feed.presentation.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import coil3.ImageLoader
import com.eric.contentfeed.core.connectivity.ConnectivityStatus
import com.eric.contentfeed.designsystem.component.ArticleRowSkeleton
import com.eric.contentfeed.designsystem.component.EmptyPanel
import com.eric.contentfeed.designsystem.component.LedgerDivider
import com.eric.contentfeed.designsystem.component.ScopedErrorPanel
import com.eric.contentfeed.designsystem.theme.ContentFeedTheme
import com.eric.contentfeed.feed.R
import com.eric.contentfeed.feed.presentation.contract.FeedContract
import com.eric.contentfeed.feed.presentation.model.FeedItemUiModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FeedScreen(
    state: FeedContract.State,
    imageLoader: ImageLoader,
    onEvent: (FeedContract.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val currentOnEvent by rememberUpdatedState(onEvent)

    LaunchedEffect(listState) {
        snapshotFlow {
            val layout = listState.layoutInfo
            layout.visibleItemsInfo.lastOrNull()?.index to layout.totalItemsCount
        }.distinctUntilChanged().collect { (lastVisible, total) ->
            if (total > 0 && lastVisible != null && lastVisible >= total - 3) {
                currentOnEvent(FeedContract.Event.LoadNextPage)
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing(),
        onRefresh = { onEvent(FeedContract.Event.Refresh) },
        modifier = modifier,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
        ) {
            item(key = "weather") {
                FeedWeatherSection(
                    state = state.weather,
                    onRetry = { onEvent(FeedContract.Event.Refresh) },
                )
            }
            when (val articles = state.articles) {
                FeedContract.ArticleStreamState.Loading ->
                    item(key = "articles-loading") {
                        repeat(3) { index ->
                            ArticleRowSkeleton()
                            if (index < 2) LedgerDivider()
                        }
                    }
                FeedContract.ArticleStreamState.Empty ->
                    item(key = "articles-empty") {
                        EmptyPanel(
                            message = stringResource(R.string.feed_empty_articles),
                            modifier = Modifier.padding(ContentFeedTheme.dimens.space4),
                        )
                    }
                FeedContract.ArticleStreamState.OfflineEmpty ->
                    item(key = "articles-offline") {
                        EmptyPanel(
                            message = stringResource(R.string.feed_offline_articles),
                            modifier = Modifier.padding(ContentFeedTheme.dimens.space4),
                        )
                    }
                is FeedContract.ArticleStreamState.Error ->
                    item(key = "articles-error") {
                        ScopedErrorPanel(
                            message = remoteFailureMessage(articles.cause),
                            retryLabel = stringResource(R.string.action_retry),
                            onRetry = { onEvent(FeedContract.Event.Refresh) },
                            modifier = Modifier.padding(ContentFeedTheme.dimens.space4),
                        )
                    }
                is FeedContract.ArticleStreamState.Content -> {
                    articles.error?.let { failure ->
                        item(key = "articles-refresh-error") {
                            ScopedErrorPanel(
                                message = remoteFailureMessage(failure),
                                retryLabel = stringResource(R.string.action_retry),
                                onRetry = { onEvent(FeedContract.Event.Refresh) },
                                modifier = Modifier.padding(ContentFeedTheme.dimens.space4),
                            )
                        }
                    }
                    itemsIndexed(
                        items = articles.items,
                        key = { _, item -> item.key },
                    ) { index, item ->
                        FeedItemRow(
                            item = item,
                            imageLoader = imageLoader,
                            canOpenExternalLinks = state.connectivityStatus == ConnectivityStatus.Online,
                            onEvent = onEvent,
                        )
                        if (index < articles.items.lastIndex) LedgerDivider()
                    }
                    item(key = "pagination") {
                        PaginationFooter(
                            state = articles.pagination,
                            onRetry = { onEvent(FeedContract.Event.RetryNextPage) },
                        )
                    }
                }
            }
        }
    }
}

private fun FeedContract.State.isRefreshing(): Boolean =
    when (val weatherState = weather) {
        FeedContract.WeatherState.Loading -> true
        is FeedContract.WeatherState.Content -> weatherState.isRefreshing
        FeedContract.WeatherState.Empty,
        is FeedContract.WeatherState.Error,
        -> false
    } ||
        when (val articleState = articles) {
            FeedContract.ArticleStreamState.Loading -> true
            is FeedContract.ArticleStreamState.Content -> articleState.isRefreshing
            FeedContract.ArticleStreamState.Empty,
            FeedContract.ArticleStreamState.OfflineEmpty,
            is FeedContract.ArticleStreamState.Error,
            -> false
        }

private val FeedItemUiModel.key: String
    get() =
        when (this) {
            is FeedItemUiModel.Article -> "article-${value.id}"
            is FeedItemUiModel.ServiceCard -> "service-${value.poolIndex}-${value.assignmentSequence}"
        }

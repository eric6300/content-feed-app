@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.feed.presentation.ui

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import com.eric.contentfeed.feed.presentation.contract.FeedContract
import com.eric.contentfeed.feed.presentation.model.FeedItemUiModel

@Composable
internal fun FeedItemRow(
    item: FeedItemUiModel,
    imageLoader: ImageLoader,
    canOpenExternalLinks: Boolean,
    onEvent: (FeedContract.Event) -> Unit,
) {
    when (item) {
        is FeedItemUiModel.Article ->
            ArticleFeedItem(
                article = item.value,
                imageLoader = imageLoader,
                onOpen = { onEvent(FeedContract.Event.OpenArticle(item.value.id)) },
                onToggleSave = { onEvent(FeedContract.Event.ToggleSave(item.value.id)) },
            )
        is FeedItemUiModel.ServiceCard ->
            ServiceFeedItem(
                serviceCard = item.value,
                imageLoader = imageLoader,
                canOpenExternalLinks = canOpenExternalLinks,
                onOpenDetails = {
                    onEvent(
                        FeedContract.Event.OpenServiceCard(
                            poolIndex = item.value.poolIndex,
                            assignmentSequence = item.value.assignmentSequence,
                        ),
                    )
                },
                onOpenExternal = {
                    onEvent(FeedContract.Event.OpenExternalLink(item.value.targetUrl))
                },
            )
    }
}

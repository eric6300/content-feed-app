@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.feed.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import com.eric.contentfeed.core.ui.click
import com.eric.contentfeed.designsystem.component.EmptyPanel
import com.eric.contentfeed.designsystem.component.LedgerDivider
import com.eric.contentfeed.designsystem.component.SourceMark
import com.eric.contentfeed.designsystem.theme.ContentFeedTheme
import com.eric.contentfeed.feed.R
import com.eric.contentfeed.feed.presentation.contract.SavedContract
import com.eric.contentfeed.feed.presentation.format.formatPublishedDate
import com.eric.contentfeed.feed.presentation.viewmodel.SavedViewModel
import org.koin.compose.koinInject
import java.io.File

@Composable
fun SavedRoute(
    viewModel: SavedViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val imageLoader: ImageLoader = koinInject()
    val dateUnknownLabel = stringResource(R.string.date_unknown)

    Column(modifier = modifier.fillMaxSize()) {
        when (val content = state.content) {
            SavedContract.ContentState.Loading ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            SavedContract.ContentState.Empty ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyPanel(
                        message = stringResource(R.string.saved_empty),
                        modifier = Modifier.fillMaxWidth().padding(ContentFeedTheme.dimens.space4),
                    )
                }
            is SavedContract.ContentState.Ready ->
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(content.items, key = { _, item -> item.id }) { index, article ->
                        ListItem(
                            modifier =
                                Modifier.click {
                                    viewModel.onEvent(SavedContract.Event.OpenArticle(article.id))
                                },
                            overlineContent = { SourceMark(source = article.source) },
                            leadingContent = {
                                FeedImage(
                                    model = article.localImagePath?.let(::File) ?: article.imageUrl,
                                    imageLoader = imageLoader,
                                    contentDescription = article.title,
                                    diskCacheKey = article.imageUrl.takeIf { article.localImagePath == null },
                                    modifier =
                                        Modifier
                                            .size(ContentFeedTheme.dimens.thumbnailSaved)
                                            .clip(MaterialTheme.shapes.medium),
                                )
                            },
                            headlineContent = {
                                Text(
                                    text = article.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            supportingContent = {
                                Text(
                                    text =
                                        formatPublishedDate(
                                            article.publishedAtEpochMillis,
                                            unknownLabel = dateUnknownLabel,
                                        ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            trailingContent = {
                                IconButton(
                                    modifier = Modifier.size(ContentFeedTheme.dimens.touchMin),
                                    onClick = {
                                        viewModel.onEvent(SavedContract.Event.RemoveArticle(article.id))
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = stringResource(R.string.saved_remove),
                                    )
                                }
                            },
                        )
                        if (index < content.items.lastIndex) {
                            LedgerDivider()
                        }
                    }
                }
        }
    }
}

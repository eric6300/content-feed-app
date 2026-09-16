@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.feed.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import com.eric.contentfeed.designsystem.component.EmptyPanel
import com.eric.contentfeed.designsystem.component.LedgerDivider
import com.eric.contentfeed.designsystem.component.SourceMark
import com.eric.contentfeed.designsystem.theme.ContentFeedTheme
import com.eric.contentfeed.feed.presentation.contract.SavedContract
import com.eric.contentfeed.feed.presentation.format.formatPublishedDate
import com.eric.contentfeed.feed.presentation.viewmodel.SavedViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.io.File

@Composable
fun SavedRoute(
    onNavigateToArticle: (Int) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val viewModel: SavedViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnNavigateToArticle by rememberUpdatedState(onNavigateToArticle)
    val imageLoader: ImageLoader = koinInject()

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is SavedContract.Effect.NavigateToArticle ->
                    currentOnNavigateToArticle(effect.articleId)
                is SavedContract.Effect.ShowUndo -> {
                    val result =
                        snackbarHostState.showSnackbar(
                            message = "Removed from Saved",
                            actionLabel = "Undo",
                        )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onEvent(SavedContract.Event.UndoRemoval(effect.articleId))
                    } else {
                        viewModel.onEvent(SavedContract.Event.UndoWindowElapsed(effect.articleId))
                    }
                    // If this coroutine is cancelled instead (composition teardown, e.g.
                    // rotation or a tab switch), the removal is deliberately left pending:
                    // the saved-list query hides it, the ViewModel's own timer and the
                    // persisted deadline both still finalize it, and undo still works
                    // until that deadline passes.
                }
                SavedContract.Effect.UndoUnavailable ->
                    snackbarHostState.showSnackbar("That article was already removed.")
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        when (val content = state.content) {
            SavedContract.ContentState.Loading ->
                CircularProgressIndicator(
                    modifier = Modifier.padding(ContentFeedTheme.dimens.space4),
                )
            SavedContract.ContentState.Empty ->
                EmptyPanel(
                    message = "No saved articles yet.",
                    modifier = Modifier.padding(ContentFeedTheme.dimens.space4),
                )
            is SavedContract.ContentState.Ready ->
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(content.items, key = { _, item -> item.id }) { index, article ->
                        ListItem(
                            modifier =
                                Modifier.clickable {
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
                                Text(article.title, style = MaterialTheme.typography.titleMedium)
                            },
                            supportingContent = {
                                Text(
                                    text = formatPublishedDate(article.publishedAtEpochMillis),
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
                                        contentDescription = "Remove from saved",
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

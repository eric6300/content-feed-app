@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.feed.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eric.contentfeed.feed.presentation.contract.SavedContract
import com.eric.contentfeed.feed.presentation.viewmodel.SavedViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun SavedRoute(
    onNavigateToArticle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SavedViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentOnNavigateToArticle by rememberUpdatedState(onNavigateToArticle)

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is SavedContract.Effect.NavigateToArticle ->
                    currentOnNavigateToArticle(effect.articleId)
                is SavedContract.Effect.ShowUndo -> {
                    var settled = false
                    try {
                        val result =
                            snackbarHostState.showSnackbar(
                                message = "Removed from Saved",
                                actionLabel = "Undo",
                            )
                        if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                            viewModel.onEvent(SavedContract.Event.UndoRemoval(effect.articleId))
                        } else {
                            viewModel.onEvent(SavedContract.Event.UndoWindowElapsed(effect.articleId))
                        }
                        settled = true
                    } finally {
                        // collectLatest and leaving composition cancel showSnackbar. Treat
                        // that cancellation as dismissal for this article only; the
                        // ViewModel's per-article timer remains the lifecycle-safe fallback.
                        if (!settled) {
                            viewModel.onEvent(SavedContract.Event.UndoWindowElapsed(effect.articleId))
                        }
                    }
                }
            }
        }
    }

    Column(modifier = modifier) {
        Text("Saved", modifier = Modifier.padding(16.dp))
        when (val content = state.content) {
            SavedContract.ContentState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            SavedContract.ContentState.Empty -> Text("No saved articles yet.", modifier = Modifier.padding(16.dp))
            is SavedContract.ContentState.Ready ->
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(content.items, key = { it.id }) { article ->
                        ListItem(
                            modifier =
                                Modifier.clickable {
                                    viewModel.onEvent(SavedContract.Event.OpenArticle(article.id))
                                },
                            headlineContent = { Text(article.title) },
                            supportingContent = { Text(article.source) },
                            trailingContent = {
                                TextButton(
                                    onClick = {
                                        viewModel.onEvent(SavedContract.Event.RemoveArticle(article.id))
                                    },
                                ) {
                                    Text("Remove")
                                }
                            },
                        )
                        HorizontalDivider()
                    }
                }
        }
        SnackbarHost(hostState = snackbarHostState)
    }
}

@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.feed.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import com.eric.contentfeed.feed.presentation.contract.FeedContract
import com.eric.contentfeed.feed.presentation.viewmodel.FeedViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun FeedRoute(
    onEffect: (FeedContract.Effect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: FeedViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val imageLoader: ImageLoader = koinInject()

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest(onEffect)
    }

    FeedScreen(
        state = state,
        imageLoader = imageLoader,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}

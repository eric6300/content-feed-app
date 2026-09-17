@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.feed.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import com.eric.contentfeed.feed.presentation.viewmodel.SavedViewModel
import org.koin.compose.koinInject

@Composable
fun SavedRoute(
    viewModel: SavedViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val imageLoader: ImageLoader = koinInject()

    SavedScreen(
        state = state,
        imageLoader = imageLoader,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}

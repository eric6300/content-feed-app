@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.feed.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import com.eric.contentfeed.feed.presentation.contract.DetailContract
import com.eric.contentfeed.feed.presentation.model.DetailTarget
import com.eric.contentfeed.feed.presentation.viewmodel.DetailViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun DetailRoute(
    target: DetailTarget,
    onEffect: (DetailContract.Effect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: DetailViewModel = koinViewModel(parameters = { parametersOf(target) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    val imageLoader: ImageLoader = koinInject()

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest(onEffect)
    }

    DetailScreen(
        state = state,
        imageLoader = imageLoader,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}

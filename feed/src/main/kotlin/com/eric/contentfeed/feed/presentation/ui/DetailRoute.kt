@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.feed.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import com.eric.contentfeed.designsystem.component.EmptyPanel
import com.eric.contentfeed.designsystem.component.KeepAction
import com.eric.contentfeed.designsystem.component.SourceMark
import com.eric.contentfeed.designsystem.component.StatusStrip
import com.eric.contentfeed.designsystem.theme.ContentFeedTheme
import com.eric.contentfeed.feed.R
import com.eric.contentfeed.feed.presentation.contract.DetailContract
import com.eric.contentfeed.feed.presentation.format.formatPrice
import com.eric.contentfeed.feed.presentation.format.formatPublishedDate
import com.eric.contentfeed.feed.presentation.model.DetailTarget
import com.eric.contentfeed.feed.presentation.model.DetailUiModel
import com.eric.contentfeed.feed.presentation.viewmodel.DetailViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import java.io.File

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

    Column(
        modifier =
            modifier
                .padding(ContentFeedTheme.dimens.space4)
                .verticalScroll(rememberScrollState()),
    ) {
        when (val content = state.content) {
            DetailContract.ContentState.Loading ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(ContentFeedTheme.dimens.space4))
                }
            DetailContract.ContentState.NotFound ->
                EmptyPanel(
                    message = stringResource(R.string.detail_not_found),
                )
            is DetailContract.ContentState.Ready -> {
                if (state.isOffline) {
                    StatusStrip(
                        message = stringResource(R.string.detail_offline_saved),
                        modifier = Modifier.padding(bottom = ContentFeedTheme.dimens.space4),
                    )
                }
                DetailContent(
                    content = content.value,
                    imageLoader = imageLoader,
                    isOffline = state.isOffline,
                    onToggleSave = { viewModel.onEvent(DetailContract.Event.ToggleSave) },
                    onOpenExternal = { viewModel.onEvent(DetailContract.Event.OpenExternalLink) },
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    content: DetailUiModel,
    imageLoader: ImageLoader,
    isOffline: Boolean,
    onToggleSave: () -> Unit,
    onOpenExternal: () -> Unit,
) {
    val dimens = ContentFeedTheme.dimens
    val dateUnknownLabel = stringResource(R.string.date_unknown)
    Column {
        when (content) {
            is DetailUiModel.Article -> {
                Text(
                    text = content.value.title,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.height(dimens.space3))
                FeedImage(
                    model = content.value.localImagePath?.let(::File) ?: content.value.imageUrl,
                    imageLoader = imageLoader,
                    contentDescription = content.value.title,
                    diskCacheKey = content.value.imageUrl.takeIf { content.value.localImagePath == null },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(dimens.detailImageHeight)
                            .clip(MaterialTheme.shapes.large),
                )
                Spacer(modifier = Modifier.height(dimens.space4))
                SourceMark(source = content.value.source)
                Spacer(modifier = Modifier.height(dimens.space2))
                Column(verticalArrangement = Arrangement.spacedBy(dimens.space1)) {
                    if (content.value.authors.isNotEmpty()) {
                        Text(
                            text =
                                stringResource(
                                    R.string.detail_by,
                                    content.value.authors.joinToString(", "),
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        text =
                            stringResource(
                                R.string.detail_published,
                                formatPublishedDate(
                                    content.value.publishedAtEpochMillis,
                                    unknownLabel = dateUnknownLabel,
                                ),
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(dimens.space5))
                Text(
                    text =
                        content.value.summary
                            ?: stringResource(R.string.article_summary_unavailable),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(dimens.space6))
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.space2),
                ) {
                    KeepAction(
                        isKept = content.value.isSaved,
                        contentDescription =
                            stringResource(
                                if (content.value.isSaved) {
                                    R.string.article_remove_from_saved
                                } else {
                                    R.string.article_save
                                },
                            ),
                        onClick = onToggleSave,
                    )
                    Button(enabled = !isOffline, onClick = onOpenExternal) {
                        Text(stringResource(R.string.detail_read_source))
                    }
                }
            }
            is DetailUiModel.ServiceCard -> {
                Text(
                    text = content.value.title,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.height(dimens.space3))
                FeedImage(
                    model = content.value.imageAssetPath.toAssetUri(),
                    imageLoader = imageLoader,
                    contentDescription = content.value.title,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(dimens.detailImageHeight)
                            .clip(MaterialTheme.shapes.large),
                )
                Spacer(modifier = Modifier.height(dimens.space4))
                Text(content.value.description, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(dimens.space2))
                Text(
                    text =
                        content.value.price?.let {
                            stringResource(R.string.detail_price, formatPrice(it))
                        } ?: stringResource(R.string.detail_price_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(dimens.space5))
                Button(enabled = !isOffline, onClick = onOpenExternal) {
                    Text(stringResource(R.string.service_view))
                }
            }
        }
    }
}

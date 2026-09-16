@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.feed.presentation.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import com.eric.contentfeed.feed.presentation.contract.DetailContract
import com.eric.contentfeed.feed.presentation.model.DetailTarget
import com.eric.contentfeed.feed.presentation.model.DetailUiModel
import com.eric.contentfeed.feed.presentation.viewmodel.DetailViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import java.io.File
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

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

    Column(modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        when (val content = state.content) {
            DetailContract.ContentState.Loading -> CircularProgressIndicator()
            DetailContract.ContentState.NotFound -> Text("This item is no longer available.")
            is DetailContract.ContentState.Ready -> {
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
    Column {
        when (content) {
            is DetailUiModel.Article -> {
                Text(content.value.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                FeedImage(
                    model = content.value.localImagePath?.let(::File) ?: content.value.imageUrl,
                    imageLoader = imageLoader,
                    contentDescription = content.value.title,
                    diskCacheKey = content.value.imageUrl.takeIf { content.value.localImagePath == null },
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(content.value.source, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text =
                        if (content.value.authors.isEmpty()) {
                            "Author unavailable"
                        } else {
                            "By ${content.value.authors.joinToString(", ")}"
                        },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Published ${formatPublishedDate(content.value.publishedAtEpochMillis)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(content.value.summary ?: "No summary available.")
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onToggleSave) {
                        Text(if (content.value.isSaved) "Remove saved" else "Save")
                    }
                    TextButton(enabled = !isOffline, onClick = onOpenExternal) { Text("Read source") }
                }
            }
            is DetailUiModel.ServiceCard -> {
                Text(content.value.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                FeedImage(
                    model = content.value.imageAssetPath.toAssetUri(),
                    imageLoader = imageLoader,
                    contentDescription = content.value.title,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(content.value.description)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = content.value.price?.let { "Price: ${formatPrice(it)}" } ?: "Price unavailable",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(enabled = !isOffline, onClick = onOpenExternal) { Text("View service") }
            }
        }
        if (isOffline) {
            Text(
                text = "Offline: saved content remains available on this device.",
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

private fun formatPublishedDate(epochMillis: Long?): String {
    if (epochMillis == null) return "date unavailable"
    return runCatching {
        DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(epochMillis))
    }.getOrDefault("date unavailable")
}

private fun formatPrice(price: Double): String =
    NumberFormat
        .getNumberInstance(Locale.getDefault())
        .apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(price)

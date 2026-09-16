@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.feed.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import com.eric.contentfeed.core.connectivity.ConnectivityStatus
import com.eric.contentfeed.designsystem.component.EmptyPanel
import com.eric.contentfeed.designsystem.component.ScopedErrorPanel
import com.eric.contentfeed.designsystem.component.WeatherPanelSkeleton
import com.eric.contentfeed.designsystem.theme.ContentFeedTheme
import com.eric.contentfeed.feed.domain.model.RemoteFailure
import com.eric.contentfeed.feed.presentation.contract.FeedContract
import com.eric.contentfeed.feed.presentation.model.FeedItemUiModel
import com.eric.contentfeed.feed.presentation.model.WeatherForecastUiModel
import com.eric.contentfeed.feed.presentation.model.WeatherUiModel
import com.eric.contentfeed.feed.presentation.viewmodel.FeedViewModel
import com.eric.contentfeed.feed.presentation.weather.WeatherConditionIcon
import com.eric.contentfeed.feed.presentation.weather.display
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedRoute(
    onEffect: (FeedContract.Effect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: FeedViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val imageLoader: ImageLoader = koinInject()

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest(onEffect)
    }
    LaunchedEffect(viewModel, listState) {
        snapshotFlow {
            val layout = listState.layoutInfo
            layout.visibleItemsInfo.lastOrNull()?.index to layout.totalItemsCount
        }.distinctUntilChanged().collect { (lastVisible, total) ->
            if (total > 0 && lastVisible != null && lastVisible >= total - 3) {
                viewModel.onEvent(FeedContract.Event.LoadNextPage)
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing(),
        onRefresh = { viewModel.onEvent(FeedContract.Event.Refresh) },
        modifier = modifier,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
        ) {
            item(key = "feed-header") {
                FeedHeader()
            }
            item(key = "weather") {
                WeatherSection(
                    state = state.weather,
                    onRetry = { viewModel.onEvent(FeedContract.Event.Refresh) },
                )
            }
            when (val articles = state.articles) {
                FeedContract.ArticleStreamState.Loading ->
                    item(key = "articles-loading") {
                        LoadingMessage("Loading articles…")
                    }
                FeedContract.ArticleStreamState.Empty ->
                    item(key = "articles-empty") {
                        EmptyMessage("No articles are available yet.")
                    }
                FeedContract.ArticleStreamState.OfflineEmpty ->
                    item(key = "articles-offline") {
                        EmptyMessage("You are offline. Previously loaded articles will appear here when available.")
                    }
                is FeedContract.ArticleStreamState.Error ->
                    item(key = "articles-error") {
                        ErrorMessage(articles.cause, onRetry = { viewModel.onEvent(FeedContract.Event.Refresh) })
                    }
                is FeedContract.ArticleStreamState.Content -> {
                    articles.error?.let { failure ->
                        item(key = "articles-refresh-error") {
                            ErrorMessage(
                                failure,
                                onRetry = { viewModel.onEvent(FeedContract.Event.Refresh) },
                            )
                        }
                    }
                    items(
                        items = articles.items,
                        key = { item -> item.key },
                    ) { item ->
                        FeedItemRow(
                            item = item,
                            imageLoader = imageLoader,
                            canOpenExternalLinks =
                                state.connectivityStatus == ConnectivityStatus.Online,
                            onOpenArticle = { viewModel.onEvent(FeedContract.Event.OpenArticle(it)) },
                            onOpenServiceCard = { poolIndex, sequence ->
                                viewModel.onEvent(FeedContract.Event.OpenServiceCard(poolIndex, sequence))
                            },
                            onOpenExternalLink = { url ->
                                viewModel.onEvent(FeedContract.Event.OpenExternalLink(url))
                            },
                            onToggleSave = { viewModel.onEvent(FeedContract.Event.ToggleSave(it)) },
                        )
                    }
                    item(key = "pagination") {
                        PaginationFooter(
                            state = articles.pagination,
                            onRetry = { viewModel.onEvent(FeedContract.Event.RetryNextPage) },
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

@Composable
private fun FeedHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Reading",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun WeatherSection(
    state: FeedContract.WeatherState,
    onRetry: () -> Unit,
) {
    when (state) {
        FeedContract.WeatherState.Loading -> WeatherPanelSkeleton()
        FeedContract.WeatherState.Empty ->
            EmptyPanel(
                message = "Weather is unavailable.",
                modifier =
                    Modifier.padding(
                        horizontal = ContentFeedTheme.dimens.space4,
                        vertical = ContentFeedTheme.dimens.space2,
                    ),
            )
        is FeedContract.WeatherState.Error ->
            ScopedErrorPanel(
                message = errorMessage(state.cause),
                retryLabel = "Retry",
                onRetry = onRetry,
                modifier =
                    Modifier.padding(
                        horizontal = ContentFeedTheme.dimens.space4,
                        vertical = ContentFeedTheme.dimens.space2,
                    ),
            )
        is FeedContract.WeatherState.Content -> {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = ContentFeedTheme.dimens.space4,
                            vertical = ContentFeedTheme.dimens.space2,
                        ),
                shape = MaterialTheme.shapes.large,
                colors =
                    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = ContentFeedTheme.dimens.elevationTonal1,
                    ),
            ) {
                Column(modifier = Modifier.padding(ContentFeedTheme.dimens.space4)) {
                    Text("Weather", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(ContentFeedTheme.dimens.space3))
                    WeatherContent(state.value)
                    state.error?.let { failure ->
                        ScopedErrorPanel(
                            message = errorMessage(failure),
                            retryLabel = "Retry",
                            onRetry = onRetry,
                            modifier = Modifier.padding(top = ContentFeedTheme.dimens.space3),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherContent(weather: WeatherUiModel) {
    val dimens = ContentFeedTheme.dimens
    val conditionDisplay = weather.condition.display()
    val conditionLabel = stringResource(conditionDisplay.labelRes)
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space3)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = weather.temperatureCelsius?.let { "${it.toInt()}°C" } ?: "—",
                style = MaterialTheme.typography.displaySmall,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(conditionLabel)
                Text(
                    text = "Local conditions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = conditionDisplay.icon.imageVector(),
                contentDescription = conditionLabel,
                modifier = Modifier.size(dimens.iconStandard),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.space3),
        ) {
            WeatherMeasure(
                label = "Feels like",
                value = weather.apparentTemperatureCelsius?.let { "${it.toInt()}°C" } ?: "—",
                modifier = Modifier.weight(1f),
            )
            WeatherMeasure(
                label = "Wind",
                value = weather.windSpeedKmh?.let { "${it.toInt()} km/h" } ?: "—",
                modifier = Modifier.weight(1f),
            )
        }
        if (weather.forecast.isNotEmpty()) {
            Text("Forecast", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(dimens.space2)) {
                items(weather.forecast) { forecast ->
                    ForecastItem(forecast)
                }
            }
        }
    }
}

@Composable
private fun WeatherMeasure(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ForecastItem(forecast: WeatherForecastUiModel) {
    val dimens = ContentFeedTheme.dimens
    val conditionLabel = stringResource(forecast.condition.display().labelRes)
    Column(
        modifier = Modifier.width(dimens.weatherForecastItemWidth),
        verticalArrangement = Arrangement.spacedBy(dimens.space1),
    ) {
        Text(forecast.date, style = MaterialTheme.typography.labelMedium)
        Text(
            text = conditionLabel,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text =
                "${forecast.temperatureMaxCelsius?.toInt() ?: "—"}° / " +
                    "${forecast.temperatureMinCelsius?.toInt() ?: "—"}°",
            style = MaterialTheme.typography.labelLarge,
        )
        forecast.precipitationProbability?.let { probability ->
            Text("Rain $probability%", style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun WeatherConditionIcon.imageVector() =
    when (this) {
        WeatherConditionIcon.Clear -> Icons.Outlined.WbSunny
        WeatherConditionIcon.PartlyCloudy -> Icons.Outlined.CloudQueue
        WeatherConditionIcon.Cloud -> Icons.Outlined.Cloud
        WeatherConditionIcon.Fog -> Icons.Outlined.Visibility
        WeatherConditionIcon.Rain -> Icons.Outlined.WaterDrop
        WeatherConditionIcon.Snow -> Icons.Outlined.AcUnit
        WeatherConditionIcon.Thunderstorm -> Icons.Outlined.Thunderstorm
        WeatherConditionIcon.Unknown -> Icons.AutoMirrored.Outlined.HelpOutline
    }

@Composable
private fun FeedItemRow(
    item: FeedItemUiModel,
    imageLoader: ImageLoader,
    canOpenExternalLinks: Boolean,
    onOpenArticle: (Int) -> Unit,
    onOpenServiceCard: (Int, Long) -> Unit,
    onOpenExternalLink: (String) -> Unit,
    onToggleSave: (Int) -> Unit,
) {
    when (item) {
        is FeedItemUiModel.Article ->
            ListItem(
                modifier = Modifier.clickable { onOpenArticle(item.value.id) },
                leadingContent = {
                    FeedImage(
                        model = item.value.localImagePath?.let(::File) ?: item.value.imageUrl,
                        imageLoader = imageLoader,
                        contentDescription = item.value.title,
                        diskCacheKey = item.value.imageUrl.takeIf { item.value.localImagePath == null },
                        modifier = Modifier.size(96.dp, 72.dp),
                    )
                },
                headlineContent = { Text(item.value.title) },
                supportingContent = {
                    Text(item.value.summary ?: item.value.source)
                },
                trailingContent = {
                    IconButton(onClick = { onToggleSave(item.value.id) }) {
                        Text(if (item.value.isSaved) "✓" else "+")
                    }
                },
            )
        is FeedItemUiModel.ServiceCard ->
            Card(
                onClick = {
                    onOpenServiceCard(item.value.poolIndex, item.value.assignmentSequence)
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    FeedImage(
                        model = item.value.imageAssetPath.toAssetUri(),
                        imageLoader = imageLoader,
                        contentDescription = item.value.title,
                        modifier = Modifier.fillMaxWidth().height(144.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(item.value.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(item.value.blurb)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        enabled = canOpenExternalLinks,
                        onClick = {
                            onOpenExternalLink(item.value.targetUrl)
                        },
                    ) {
                        Text("View service")
                    }
                }
            }
    }
    HorizontalDivider()
}

@Composable
private fun PaginationFooter(
    state: FeedContract.PaginationState,
    onRetry: () -> Unit,
) {
    when (state) {
        FeedContract.PaginationState.Idle -> Unit
        FeedContract.PaginationState.Loading ->
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp).size(24.dp),
            )
        is FeedContract.PaginationState.RetryableError ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Could not load more articles.", modifier = Modifier.weight(1f))
                TextButton(onClick = onRetry) { Text("Retry") }
            }
        FeedContract.PaginationState.End ->
            Text(
                text = "You are all caught up.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.labelMedium,
            )
    }
}

@Composable
private fun LoadingMessage(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
        Text(message)
    }
}

@Composable
private fun EmptyMessage(message: String) {
    Text(message, modifier = Modifier.padding(16.dp))
}

@Composable
private fun ErrorMessage(
    failure: RemoteFailure,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(errorMessage(failure), modifier = Modifier.weight(1f))
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}

private val FeedItemUiModel.key: String
    get() =
        when (this) {
            is FeedItemUiModel.Article -> "article-${value.id}"
            is FeedItemUiModel.ServiceCard ->
                "service-${value.poolIndex}-${value.assignmentSequence}"
        }

private fun errorMessage(failure: RemoteFailure): String =
    when (failure) {
        RemoteFailure.NetworkUnavailable -> "Network unavailable."
        is RemoteFailure.Http -> "The service returned HTTP ${failure.code}."
        RemoteFailure.Unknown -> "Something went wrong."
    }

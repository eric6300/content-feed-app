@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.feed.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import com.eric.contentfeed.core.connectivity.ConnectivityStatus
import com.eric.contentfeed.core.ui.click
import com.eric.contentfeed.designsystem.component.ArticleRowSkeleton
import com.eric.contentfeed.designsystem.component.EmptyPanel
import com.eric.contentfeed.designsystem.component.KeepAction
import com.eric.contentfeed.designsystem.component.LedgerDivider
import com.eric.contentfeed.designsystem.component.ScopedErrorPanel
import com.eric.contentfeed.designsystem.component.SourceMark
import com.eric.contentfeed.designsystem.component.WeatherPanelSkeleton
import com.eric.contentfeed.designsystem.theme.ContentFeedTheme
import com.eric.contentfeed.feed.R
import com.eric.contentfeed.feed.domain.model.RemoteFailure
import com.eric.contentfeed.feed.presentation.contract.FeedContract
import com.eric.contentfeed.feed.presentation.format.formatPrice
import com.eric.contentfeed.feed.presentation.format.formatPublishedDate
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
            item(key = "weather") {
                WeatherSection(
                    state = state.weather,
                    onRetry = { viewModel.onEvent(FeedContract.Event.Refresh) },
                )
            }
            when (val articles = state.articles) {
                FeedContract.ArticleStreamState.Loading ->
                    item(key = "articles-loading") {
                        repeat(3) { index ->
                            ArticleRowSkeleton()
                            if (index < 2) {
                                LedgerDivider()
                            }
                        }
                    }
                FeedContract.ArticleStreamState.Empty ->
                    item(key = "articles-empty") {
                        EmptyPanel(
                            message = stringResource(R.string.feed_empty_articles),
                            modifier = Modifier.padding(ContentFeedTheme.dimens.space4),
                        )
                    }
                FeedContract.ArticleStreamState.OfflineEmpty ->
                    item(key = "articles-offline") {
                        EmptyPanel(
                            message = stringResource(R.string.feed_offline_articles),
                            modifier = Modifier.padding(ContentFeedTheme.dimens.space4),
                        )
                    }
                is FeedContract.ArticleStreamState.Error ->
                    item(key = "articles-error") {
                        ScopedErrorPanel(
                            message = errorMessage(articles.cause),
                            retryLabel = stringResource(R.string.action_retry),
                            onRetry = { viewModel.onEvent(FeedContract.Event.Refresh) },
                            modifier = Modifier.padding(ContentFeedTheme.dimens.space4),
                        )
                    }
                is FeedContract.ArticleStreamState.Content -> {
                    articles.error?.let { failure ->
                        item(key = "articles-refresh-error") {
                            ScopedErrorPanel(
                                message = errorMessage(failure),
                                retryLabel = stringResource(R.string.action_retry),
                                onRetry = { viewModel.onEvent(FeedContract.Event.Refresh) },
                                modifier = Modifier.padding(ContentFeedTheme.dimens.space4),
                            )
                        }
                    }
                    itemsIndexed(
                        items = articles.items,
                        key = { _, item -> item.key },
                    ) { index, item ->
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
                        if (index < articles.items.lastIndex) {
                            LedgerDivider()
                        }
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
private fun WeatherSection(
    state: FeedContract.WeatherState,
    onRetry: () -> Unit,
) {
    when (state) {
        FeedContract.WeatherState.Loading -> WeatherPanelSkeleton()
        FeedContract.WeatherState.Empty ->
            EmptyPanel(
                message = stringResource(R.string.weather_unavailable),
                modifier =
                    Modifier.padding(
                        horizontal = ContentFeedTheme.dimens.space4,
                        vertical = ContentFeedTheme.dimens.space2,
                    ),
            )
        is FeedContract.WeatherState.Error ->
            ScopedErrorPanel(
                message = errorMessage(state.cause),
                retryLabel = stringResource(R.string.action_retry),
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
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = ContentFeedTheme.dimens.elevationTonal1,
                    ),
            ) {
                Column(modifier = Modifier.padding(ContentFeedTheme.dimens.space4)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.weather_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        SourceMark(source = stringResource(R.string.weather_source))
                    }
                    Spacer(modifier = Modifier.height(ContentFeedTheme.dimens.space3))
                    WeatherContent(state.value)
                    state.error?.let { failure ->
                        ScopedErrorPanel(
                            message = errorMessage(failure),
                            retryLabel = stringResource(R.string.action_retry),
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
    val unavailableValue = stringResource(R.string.value_unavailable)
    val temperatureLabel =
        weather.temperatureCelsius?.let {
            stringResource(R.string.weather_temperature_celsius, it.toInt())
        } ?: unavailableValue
    val feelsLike =
        weather.apparentTemperatureCelsius?.let {
            stringResource(R.string.weather_temperature_celsius, it.toInt())
        } ?: unavailableValue
    val wind =
        weather.windSpeedKmh?.let {
            stringResource(R.string.weather_wind_speed, it.toInt())
        } ?: unavailableValue
    BoxWithConstraints {
        val isExpanded = maxWidth >= dimens.weatherExpandedBreakpoint
        Column(verticalArrangement = Arrangement.spacedBy(dimens.space3)) {
            if (isExpanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimens.space4),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WeatherPrimaryContent(
                        temperatureLabel = temperatureLabel,
                        conditionLabel = conditionLabel,
                        conditionIcon = conditionDisplay.icon.imageVector(),
                        modifier = Modifier.weight(1.35f),
                    )
                    WeatherMeasure(
                        label = stringResource(R.string.weather_feels_like),
                        value = feelsLike,
                        modifier = Modifier.weight(0.6f),
                    )
                    WeatherMeasure(
                        label = stringResource(R.string.weather_wind),
                        value = wind,
                        modifier = Modifier.weight(0.6f),
                    )
                }
            } else {
                WeatherPrimaryContent(
                    temperatureLabel = temperatureLabel,
                    conditionLabel = conditionLabel,
                    conditionIcon = conditionDisplay.icon.imageVector(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimens.space3),
                ) {
                    WeatherMeasure(
                        label = stringResource(R.string.weather_feels_like),
                        value = feelsLike,
                        modifier = Modifier.weight(1f),
                    )
                    WeatherMeasure(
                        label = stringResource(R.string.weather_wind),
                        value = wind,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (weather.forecast.isNotEmpty()) {
                Text(
                    stringResource(R.string.weather_forecast),
                    style = MaterialTheme.typography.titleMedium,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(dimens.space2)) {
                    items(weather.forecast) { forecast ->
                        ForecastItem(forecast)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherPrimaryContent(
    temperatureLabel: String,
    conditionLabel: String,
    conditionIcon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val dimens = ContentFeedTheme.dimens
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dimens.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = temperatureLabel,
            style = MaterialTheme.typography.displaySmall,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(conditionLabel)
            Text(
                text = stringResource(R.string.weather_local_conditions),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Icon(
            imageVector = conditionIcon,
            contentDescription = conditionLabel,
            modifier = Modifier.size(dimens.iconStandard),
        )
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
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(text = value, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ForecastItem(forecast: WeatherForecastUiModel) {
    val dimens = ContentFeedTheme.dimens
    val conditionLabel = stringResource(forecast.condition.display().labelRes)
    val unavailableValue = stringResource(R.string.value_unavailable)
    val maximum = forecast.temperatureMaxCelsius?.toInt()?.toString() ?: unavailableValue
    val minimum = forecast.temperatureMinCelsius?.toInt()?.toString() ?: unavailableValue
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
            text = stringResource(R.string.weather_temperature_range, maximum, minimum),
            style = MaterialTheme.typography.labelLarge,
        )
        forecast.precipitationProbability?.let { probability ->
            Text(
                stringResource(R.string.weather_rain_probability, probability),
                style = MaterialTheme.typography.labelSmall,
            )
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
    val dimens = ContentFeedTheme.dimens
    val dateUnknownLabel = stringResource(R.string.date_unknown)
    when (item) {
        is FeedItemUiModel.Article ->
            ListItem(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .click { onOpenArticle(item.value.id) },
                overlineContent = {
                    SourceMark(source = item.value.source)
                },
                leadingContent = {
                    FeedImage(
                        model = item.value.localImagePath?.let(::File) ?: item.value.imageUrl,
                        imageLoader = imageLoader,
                        contentDescription = item.value.title,
                        diskCacheKey = item.value.imageUrl.takeIf { item.value.localImagePath == null },
                        modifier = Modifier.size(dimens.thumbnailArticle).clip(MaterialTheme.shapes.medium),
                    )
                },
                headlineContent = {
                    Text(
                        text = item.value.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(dimens.space1)) {
                        Text(
                            text =
                                item.value.summary
                                    ?: stringResource(R.string.article_summary_unavailable),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text =
                                formatPublishedDate(
                                    item.value.publishedAtEpochMillis,
                                    unknownLabel = dateUnknownLabel,
                                ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                trailingContent = {
                    KeepAction(
                        isKept = item.value.isSaved,
                        contentDescription =
                            stringResource(
                                if (item.value.isSaved) {
                                    R.string.article_remove_from_saved
                                } else {
                                    R.string.article_save
                                },
                            ),
                        onClick = { onToggleSave(item.value.id) },
                    )
                },
            )
        is FeedItemUiModel.ServiceCard ->
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = dimens.space4,
                            vertical = dimens.space3,
                        ),
            ) {
                androidx.compose.material3.Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = dimens.elevationTonal1,
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .click {
                                    onOpenServiceCard(
                                        item.value.poolIndex,
                                        item.value.assignmentSequence,
                                    )
                                },
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(dimens.space4),
                        ) {
                            FeedImage(
                                model = item.value.imageAssetPath.toAssetUri(),
                                imageLoader = imageLoader,
                                contentDescription = item.value.title,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(dimens.serviceImageHeight)
                                        .clip(MaterialTheme.shapes.medium),
                            )
                            Spacer(modifier = Modifier.height(dimens.space3))
                            Text(item.value.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(dimens.space1))
                            Text(item.value.blurb, style = MaterialTheme.typography.bodyLarge)
                            item.value.price?.let { price ->
                                Spacer(modifier = Modifier.height(dimens.space2))
                                Text(
                                    text =
                                        stringResource(
                                            R.string.service_price,
                                            formatPrice(price),
                                        ),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                        Button(
                            modifier =
                                Modifier.padding(
                                    start = dimens.space4,
                                    bottom = dimens.space4,
                                ),
                            enabled = canOpenExternalLinks,
                            onClick = { onOpenExternalLink(item.value.targetUrl) },
                        ) { Text(stringResource(R.string.service_view)) }
                    }
                }
            }
    }
}

@Composable
private fun PaginationFooter(
    state: FeedContract.PaginationState,
    onRetry: () -> Unit,
) {
    when (state) {
        FeedContract.PaginationState.Idle -> Unit
        FeedContract.PaginationState.Loading ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxWidth().padding(ContentFeedTheme.dimens.space4),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(ContentFeedTheme.dimens.iconStandard))
            }
        is FeedContract.PaginationState.RetryableError ->
            ScopedErrorPanel(
                message = errorMessage(state.cause),
                retryLabel = stringResource(R.string.action_retry),
                onRetry = onRetry,
                modifier = Modifier.padding(ContentFeedTheme.dimens.space4),
            )
        FeedContract.PaginationState.End ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxWidth().padding(ContentFeedTheme.dimens.space4),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.pagination_caught_up),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
    }
}

private val FeedItemUiModel.key: String
    get() =
        when (this) {
            is FeedItemUiModel.Article -> "article-${value.id}"
            is FeedItemUiModel.ServiceCard ->
                "service-${value.poolIndex}-${value.assignmentSequence}"
        }

@Composable
private fun errorMessage(failure: RemoteFailure): String =
    when (failure) {
        RemoteFailure.NetworkUnavailable -> stringResource(R.string.error_network_unavailable)
        is RemoteFailure.Http -> stringResource(R.string.error_http, failure.code)
        RemoteFailure.Unknown -> stringResource(R.string.error_unknown)
    }

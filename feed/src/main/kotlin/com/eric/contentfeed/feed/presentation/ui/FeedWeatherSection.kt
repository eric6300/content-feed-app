@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.feed.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.eric.contentfeed.designsystem.component.EmptyPanel
import com.eric.contentfeed.designsystem.component.ScopedErrorPanel
import com.eric.contentfeed.designsystem.component.SourceMark
import com.eric.contentfeed.designsystem.component.WeatherPanelSkeleton
import com.eric.contentfeed.designsystem.theme.ContentFeedTheme
import com.eric.contentfeed.feed.R
import com.eric.contentfeed.feed.domain.model.RemoteFailure
import com.eric.contentfeed.feed.presentation.contract.FeedContract
import com.eric.contentfeed.feed.presentation.model.WeatherForecastUiModel
import com.eric.contentfeed.feed.presentation.model.WeatherUiModel
import com.eric.contentfeed.feed.presentation.weather.WeatherConditionIcon
import com.eric.contentfeed.feed.presentation.weather.display

@Composable
internal fun FeedWeatherSection(
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
                message = remoteFailureMessage(state.cause),
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
                            message = remoteFailureMessage(failure),
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
                    WeatherMeasures(feelsLike = feelsLike, wind = wind, weight = 0.6f)
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
                    WeatherMeasures(feelsLike = feelsLike, wind = wind, weight = 1f)
                }
            }
            if (weather.forecast.isNotEmpty()) {
                Text(
                    stringResource(R.string.weather_forecast),
                    style = MaterialTheme.typography.titleMedium,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(dimens.space2)) {
                    items(weather.forecast) { forecast -> ForecastItem(forecast) }
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
        Text(text = temperatureLabel, style = MaterialTheme.typography.displaySmall)
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
private fun RowScope.WeatherMeasures(
    feelsLike: String,
    wind: String,
    weight: Float,
) {
    WeatherMeasure(
        label = stringResource(R.string.weather_feels_like),
        value = feelsLike,
        modifier = Modifier.weight(weight),
    )
    WeatherMeasure(
        label = stringResource(R.string.weather_wind),
        value = wind,
        modifier = Modifier.weight(weight),
    )
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
        Text(text = conditionLabel, style = MaterialTheme.typography.bodyMedium)
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
internal fun remoteFailureMessage(failure: RemoteFailure): String =
    when (failure) {
        RemoteFailure.NetworkUnavailable -> stringResource(R.string.error_network_unavailable)
        is RemoteFailure.Http -> stringResource(R.string.error_http, failure.code)
        RemoteFailure.Unknown -> stringResource(R.string.error_unknown)
    }

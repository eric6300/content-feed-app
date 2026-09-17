@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.feed.presentation.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import com.eric.contentfeed.designsystem.theme.ContentFeedTheme
import com.eric.contentfeed.feed.presentation.contract.FeedContract

@Composable
internal fun rememberPreviewImageLoader(): ImageLoader {
    val context = LocalPlatformContext.current
    return remember(context) { ImageLoader.Builder(context).build() }
}

@Preview(showBackground = true)
@Composable
private fun FeedScreenPreview() {
    ContentFeedTheme(darkTheme = false) {
        FeedScreen(
            state = UiPreviewFixtures.feedState,
            imageLoader = rememberPreviewImageLoader(),
            onEvent = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WeatherSectionPreview() {
    ContentFeedTheme(darkTheme = false) {
        FeedWeatherSection(
            state =
                FeedContract.WeatherState.Content(
                    value = UiPreviewFixtures.weather,
                    isRefreshing = false,
                    error = null,
                ),
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ArticleFeedItemPreview() {
    ContentFeedTheme(darkTheme = false) {
        ArticleFeedItem(
            article = UiPreviewFixtures.article,
            imageLoader = rememberPreviewImageLoader(),
            onOpen = {},
            onToggleSave = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ServiceFeedItemPreview() {
    ContentFeedTheme(darkTheme = false) {
        ServiceFeedItem(
            serviceCard = UiPreviewFixtures.serviceCard,
            imageLoader = rememberPreviewImageLoader(),
            canOpenExternalLinks = true,
            onOpenDetails = {},
            onOpenExternal = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SavedEmptyPreview() {
    ContentFeedTheme(darkTheme = false) {
        SavedScreen(
            state = UiPreviewFixtures.savedEmptyState,
            imageLoader = rememberPreviewImageLoader(),
            onEvent = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SavedPopulatedPreview() {
    ContentFeedTheme(darkTheme = false) {
        SavedScreen(
            state = UiPreviewFixtures.savedReadyState,
            imageLoader = rememberPreviewImageLoader(),
            onEvent = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ArticleDetailPreview() {
    ContentFeedTheme(darkTheme = false) {
        DetailScreen(
            state = UiPreviewFixtures.articleDetailState,
            imageLoader = rememberPreviewImageLoader(),
            onEvent = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ServiceDetailPreview() {
    ContentFeedTheme(darkTheme = false) {
        DetailScreen(
            state = UiPreviewFixtures.serviceDetailState,
            imageLoader = rememberPreviewImageLoader(),
            onEvent = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

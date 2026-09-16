@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.feed.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest

@Composable
internal fun FeedImage(
    model: Any?,
    imageLoader: ImageLoader,
    contentDescription: String,
    modifier: Modifier = Modifier,
    diskCacheKey: String? = null,
) {
    if (model == null) {
        ImagePlaceholder(
            message = "No image available",
            modifier = modifier,
        )
    } else {
        val context = LocalPlatformContext.current
        val imageRequest =
            remember(model, diskCacheKey, context) {
                ImageRequest
                    .Builder(context)
                    .data(model)
                    .apply { diskCacheKey?.let(::diskCacheKey) }
                    .build()
            }
        SubcomposeAsyncImage(
            model = imageRequest,
            imageLoader = imageLoader,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
            loading = {
                ImagePlaceholder("Loading image", Modifier.fillMaxSize())
            },
            error = {
                ImagePlaceholder("Image unavailable", Modifier.fillMaxSize())
            },
        )
    }
}

@Composable
private fun ImagePlaceholder(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

internal fun String.toAssetUri(): String = "file:///android_asset/$this"

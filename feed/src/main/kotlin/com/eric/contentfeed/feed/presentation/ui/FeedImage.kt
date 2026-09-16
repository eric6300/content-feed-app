@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.feed.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.times
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import com.eric.contentfeed.designsystem.theme.ContentFeedTheme
import com.eric.contentfeed.feed.R

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
            message = stringResource(R.string.image_no_available),
            icon = Icons.Outlined.Image,
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
                ImagePlaceholder(
                    message = stringResource(R.string.image_loading),
                    icon = Icons.Outlined.Image,
                    modifier = Modifier.fillMaxSize(),
                )
            },
            error = {
                ImagePlaceholder(
                    message = stringResource(R.string.image_unavailable),
                    icon = Icons.Outlined.BrokenImage,
                    modifier = Modifier.fillMaxSize(),
                )
            },
        )
    }
}

@Composable
private fun ImagePlaceholder(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clearAndSetSemantics { contentDescription = message },
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val dimens = ContentFeedTheme.dimens
            val showLabel = maxWidth >= dimens.thumbnailSaved.width * 2
            Column(
                modifier = Modifier.padding(dimens.space2),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(dimens.iconStandard),
                )
                if (showLabel) {
                    Spacer(modifier = Modifier.height(dimens.space1))
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

internal fun String.toAssetUri(): String = "file:///android_asset/$this"

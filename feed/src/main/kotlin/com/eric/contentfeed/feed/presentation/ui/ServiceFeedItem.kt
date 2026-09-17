@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.feed.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import coil3.ImageLoader
import com.eric.contentfeed.core.ui.click
import com.eric.contentfeed.designsystem.theme.ContentFeedTheme
import com.eric.contentfeed.feed.R
import com.eric.contentfeed.feed.presentation.format.formatPrice
import com.eric.contentfeed.feed.presentation.model.ServiceCardUiModel

@Composable
internal fun ServiceFeedItem(
    serviceCard: ServiceCardUiModel,
    imageLoader: ImageLoader,
    canOpenExternalLinks: Boolean,
    onOpenDetails: () -> Unit,
    onOpenExternal: () -> Unit,
) {
    val dimens = ContentFeedTheme.dimens
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.space4, vertical = dimens.space3),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = MaterialTheme.shapes.large,
            tonalElevation = dimens.elevationTonal1,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().click(onClick = onOpenDetails),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(dimens.space4)) {
                    FeedImage(
                        model = serviceCard.imageAssetPath.toAssetUri(),
                        imageLoader = imageLoader,
                        contentDescription = serviceCard.title,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(dimens.serviceImageHeight)
                                .clip(MaterialTheme.shapes.medium),
                    )
                    Spacer(modifier = Modifier.height(dimens.space3))
                    Text(serviceCard.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(dimens.space1))
                    Text(serviceCard.blurb, style = MaterialTheme.typography.bodyLarge)
                    serviceCard.price?.let { price ->
                        Spacer(modifier = Modifier.height(dimens.space2))
                        Text(
                            text = stringResource(R.string.service_price, formatPrice(price)),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                Button(
                    modifier = Modifier.padding(start = dimens.space4, bottom = dimens.space4),
                    enabled = canOpenExternalLinks,
                    onClick = onOpenExternal,
                ) {
                    Text(stringResource(R.string.service_view))
                }
            }
        }
    }
}

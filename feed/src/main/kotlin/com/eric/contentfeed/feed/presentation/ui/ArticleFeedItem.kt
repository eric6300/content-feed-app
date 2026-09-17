@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.feed.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import coil3.ImageLoader
import com.eric.contentfeed.core.ui.click
import com.eric.contentfeed.designsystem.component.KeepAction
import com.eric.contentfeed.designsystem.component.SourceMark
import com.eric.contentfeed.designsystem.theme.ContentFeedTheme
import com.eric.contentfeed.feed.R
import com.eric.contentfeed.feed.presentation.format.formatPublishedDate
import com.eric.contentfeed.feed.presentation.model.ArticleUiModel
import java.io.File

@Composable
internal fun ArticleFeedItem(
    article: ArticleUiModel,
    imageLoader: ImageLoader,
    onOpen: () -> Unit,
    onToggleSave: () -> Unit,
) {
    val dimens = ContentFeedTheme.dimens
    val dateUnknownLabel = stringResource(R.string.date_unknown)
    ListItem(
        modifier = Modifier.fillMaxWidth().click(onClick = onOpen),
        overlineContent = { SourceMark(source = article.source) },
        leadingContent = {
            FeedImage(
                model = article.localImagePath?.let(::File) ?: article.imageUrl,
                imageLoader = imageLoader,
                contentDescription = article.title,
                diskCacheKey = article.imageUrl.takeIf { article.localImagePath == null },
                modifier = Modifier.size(dimens.thumbnailArticle).clip(MaterialTheme.shapes.medium),
            )
        },
        headlineContent = {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(dimens.space1)) {
                Text(
                    text = article.summary ?: stringResource(R.string.article_summary_unavailable),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text =
                        formatPublishedDate(
                            article.publishedAtEpochMillis,
                            unknownLabel = dateUnknownLabel,
                        ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            KeepAction(
                isKept = article.isSaved,
                contentDescription =
                    stringResource(
                        if (article.isSaved) R.string.article_remove_from_saved else R.string.article_save,
                    ),
                onClick = onToggleSave,
            )
        },
    )
}

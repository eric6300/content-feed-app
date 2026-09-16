@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.eric.contentfeed.designsystem.theme.ContentFeedTheme

@Composable
fun SkeletonBlock(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(ContentFeedTheme.dimens.space2))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    )
}

@Composable
fun ArticleRowSkeleton(modifier: Modifier = Modifier) {
    val dimens = ContentFeedTheme.dimens
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimens.space4,
                    vertical = dimens.space3,
                ),
        horizontalArrangement = Arrangement.spacedBy(dimens.space3),
    ) {
        SkeletonBlock(modifier = Modifier.size(dimens.thumbnailArticle))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimens.space2),
        ) {
            SkeletonBlock(modifier = Modifier.fillMaxWidth().height(dimens.space4))
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.82f).height(dimens.space3))
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.56f).height(dimens.space3))
        }
        SkeletonBlock(modifier = Modifier.size(dimens.touchMin))
    }
}

@Composable
fun WeatherPanelSkeleton(modifier: Modifier = Modifier) {
    val dimens = ContentFeedTheme.dimens
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimens.space4,
                    vertical = dimens.space2,
                ),
    ) {
        Column(
            modifier = Modifier.padding(dimens.space4),
            verticalArrangement = Arrangement.spacedBy(dimens.space3),
        ) {
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.28f).height(dimens.space4))
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.space3)) {
                SkeletonBlock(modifier = Modifier.size(dimens.space7, dimens.space6))
                Column(verticalArrangement = Arrangement.spacedBy(dimens.space2)) {
                    SkeletonBlock(modifier = Modifier.size(dimens.space7, dimens.space3))
                    SkeletonBlock(modifier = Modifier.size(dimens.space7, dimens.space3))
                }
            }
            SkeletonBlock(modifier = Modifier.fillMaxWidth().height(dimens.space6))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SkeletonsPreview() {
    ContentFeedTheme {
        Column {
            WeatherPanelSkeleton()
            ArticleRowSkeleton()
        }
    }
}

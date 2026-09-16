@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.eric.contentfeed.designsystem.R
import com.eric.contentfeed.designsystem.theme.ContentFeedTheme

@Composable
fun StatusStrip(
    message: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        tonalElevation = ContentFeedTheme.dimens.elevationTonal1,
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = ContentFeedTheme.dimens.space4,
                    vertical = ContentFeedTheme.dimens.space2,
                ),
            horizontalArrangement = Arrangement.spacedBy(ContentFeedTheme.dimens.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = null,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusStripPreview() {
    ContentFeedTheme {
        StatusStrip(message = stringResource(R.string.designsystem_preview_offline))
    }
}

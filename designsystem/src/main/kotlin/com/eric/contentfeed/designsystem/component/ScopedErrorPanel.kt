@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
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
fun ScopedErrorPanel(
    message: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(
            modifier =
                Modifier.padding(
                    horizontal = ContentFeedTheme.dimens.space4,
                    vertical = ContentFeedTheme.dimens.space3,
                ),
            verticalArrangement = Arrangement.spacedBy(ContentFeedTheme.dimens.space2),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(ContentFeedTheme.dimens.space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                )
                Text(
                    text = message,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(
                modifier = Modifier.align(Alignment.End),
                onClick = onRetry,
            ) {
                Text(retryLabel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScopedErrorPanelPreview() {
    ContentFeedTheme {
        ScopedErrorPanel(
            message = stringResource(R.string.designsystem_preview_error),
            retryLabel = stringResource(R.string.designsystem_preview_retry),
            onRetry = {},
        )
    }
}

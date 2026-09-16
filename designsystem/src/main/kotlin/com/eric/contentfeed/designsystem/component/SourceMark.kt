@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.eric.contentfeed.designsystem.R
import com.eric.contentfeed.designsystem.theme.ContentFeedTheme
import com.eric.contentfeed.designsystem.theme.SignalPillShape

@Composable
fun SourceMark(
    source: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = SignalPillShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = source,
            style = MaterialTheme.typography.labelMedium,
            modifier =
                Modifier.padding(
                    horizontal = ContentFeedTheme.dimens.space2,
                    vertical = ContentFeedTheme.dimens.space1,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SourceMarkPreview() {
    ContentFeedTheme {
        SourceMark(source = stringResource(R.string.designsystem_preview_source))
    }
}

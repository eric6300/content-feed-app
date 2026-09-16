@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.eric.contentfeed.designsystem.R
import com.eric.contentfeed.designsystem.theme.ContentFeedTheme

@Composable
fun KeepAction(
    isKept: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            imageVector = if (isKept) Icons.Rounded.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = contentDescription,
            tint = if (isKept) ContentFeedTheme.extendedColors.success else Color.Unspecified,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun KeepActionPreview() {
    ContentFeedTheme {
        KeepAction(
            isKept = true,
            contentDescription = stringResource(R.string.designsystem_preview_remove_saved),
            onClick = {},
        )
    }
}

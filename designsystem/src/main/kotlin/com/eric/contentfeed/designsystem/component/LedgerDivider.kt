@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.eric.contentfeed.designsystem.theme.ContentFeedTheme

@Composable
fun LedgerDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = ContentFeedTheme.dimens.divider,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

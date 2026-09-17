@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed.feed.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.eric.contentfeed.designsystem.component.ScopedErrorPanel
import com.eric.contentfeed.designsystem.theme.ContentFeedTheme
import com.eric.contentfeed.feed.R
import com.eric.contentfeed.feed.presentation.contract.FeedContract

@Composable
internal fun PaginationFooter(
    state: FeedContract.PaginationState,
    onRetry: () -> Unit,
) {
    when (state) {
        FeedContract.PaginationState.Idle -> Unit
        FeedContract.PaginationState.Loading ->
            Box(
                modifier = Modifier.fillMaxWidth().padding(ContentFeedTheme.dimens.space4),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(ContentFeedTheme.dimens.iconStandard))
            }
        is FeedContract.PaginationState.RetryableError ->
            ScopedErrorPanel(
                message = remoteFailureMessage(state.cause),
                retryLabel = stringResource(R.string.action_retry),
                onRetry = onRetry,
                modifier = Modifier.padding(ContentFeedTheme.dimens.space4),
            )
        FeedContract.PaginationState.End ->
            Box(
                modifier = Modifier.fillMaxWidth().padding(ContentFeedTheme.dimens.space4),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.pagination_caught_up),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
    }
}

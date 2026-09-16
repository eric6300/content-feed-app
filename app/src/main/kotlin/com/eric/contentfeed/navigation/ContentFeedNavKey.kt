package com.eric.contentfeed.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface ContentFeedNavKey : NavKey {
    @Serializable
    data object Reading : ContentFeedNavKey

    @Serializable
    data object Saved : ContentFeedNavKey

    @Serializable
    data class ArticleDetail(
        val articleId: Int,
    ) : ContentFeedNavKey

    @Serializable
    data class ServiceCardDetail(
        val poolIndex: Int,
        val assignmentSequence: Long,
    ) : ContentFeedNavKey
}

package com.eric.contentfeed.feed.domain.model

data class ServiceCard(
    val id: Int,
    val title: String,
    val description: String,
    val blurb: String,
    val price: Double?,
    val imageAssetPath: String,
    val targetUrl: String,
)

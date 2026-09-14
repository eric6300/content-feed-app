package com.eric.contentfeed.feed.data.local

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

internal class ArticleAuthorsJsonCodec(
    moshi: Moshi,
) {
    private val adapter =
        moshi.adapter<List<String>>(
            Types.newParameterizedType(List::class.java, String::class.java),
        )

    fun encode(authors: List<String>): String = adapter.toJson(authors)

    fun decode(json: String): List<String> =
        runCatching {
            adapter.fromJson(json).orEmpty().filter(String::isNotBlank)
        }.getOrDefault(emptyList())
}

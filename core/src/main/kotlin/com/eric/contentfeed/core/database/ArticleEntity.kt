package com.eric.contentfeed.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val source: String,
    val authorsJson: String,
    val summary: String?,
    val imageUrl: String?,
    val articleUrl: String,
    val publishedAtEpochMillis: Long?,
    val fetchedAtEpochMillis: Long,
    val isSaved: Boolean = false,
    val savedAtEpochMillis: Long? = null,
    val localImagePath: String? = null,
    val pendingUnsaveAtEpochMillis: Long? = null,
)

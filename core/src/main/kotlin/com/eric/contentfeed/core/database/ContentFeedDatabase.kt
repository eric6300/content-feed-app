package com.eric.contentfeed.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ArticleEntity::class,
        WeatherCacheEntity::class,
        FeedPlacementEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class ContentFeedDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao

    abstract fun weatherDao(): WeatherDao

    abstract fun feedPlacementDao(): FeedPlacementDao
}

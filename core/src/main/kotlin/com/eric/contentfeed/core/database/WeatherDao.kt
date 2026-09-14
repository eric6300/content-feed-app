package com.eric.contentfeed.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather_cache WHERE cacheKey = :cacheKey LIMIT 1")
    fun observeWeather(cacheKey: String = DEFAULT_WEATHER_CACHE_KEY): Flow<WeatherCacheEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeather(cache: WeatherCacheEntity)

    @Query("DELETE FROM weather_cache WHERE cacheKey = :cacheKey")
    suspend fun deleteWeather(cacheKey: String = DEFAULT_WEATHER_CACHE_KEY)
}

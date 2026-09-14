package com.eric.contentfeed.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeatherDaoTest {
    private lateinit var database: ContentFeedDatabase
    private lateinit var dao: WeatherDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ContentFeedDatabase::class.java).build()
        dao = database.weatherDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertReplacesTheSingleDefaultWeatherSnapshot() =
        runTest {
            dao.upsertWeather(
                WeatherCacheEntity(
                    currentTemperatureCelsius = 20.0,
                    currentApparentTemperatureCelsius = 19.0,
                    currentWeatherCode = 1,
                    currentWindSpeedKmh = 4.0,
                    forecastJson = "[]",
                ),
            )
            dao.upsertWeather(
                WeatherCacheEntity(
                    currentTemperatureCelsius = null,
                    currentApparentTemperatureCelsius = null,
                    currentWeatherCode = null,
                    currentWindSpeedKmh = null,
                    forecastJson = "[]",
                ),
            )

            assertNull(dao.observeWeather().first()!!.currentTemperatureCelsius)
        }
}

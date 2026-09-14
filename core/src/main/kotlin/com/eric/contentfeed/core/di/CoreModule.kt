package com.eric.contentfeed.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.eric.contentfeed.core.database.ContentFeedDatabase
import com.eric.contentfeed.core.freshness.DataStoreFreshnessGate
import com.eric.contentfeed.core.freshness.EpochClock
import com.eric.contentfeed.core.freshness.FreshnessGate
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private val Context.freshnessDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "freshness",
)

val coreModule =
    module {
        single<ContentFeedDatabase> {
            Room
                .databaseBuilder(
                    androidContext(),
                    ContentFeedDatabase::class.java,
                    "content_feed.db",
                ).build()
        }
        single { get<ContentFeedDatabase>().articleDao() }
        single { get<ContentFeedDatabase>().weatherDao() }
        single { get<ContentFeedDatabase>().feedPlacementDao() }
        single<DataStore<Preferences>> { androidContext().freshnessDataStore }
        single<EpochClock> { EpochClock { System.currentTimeMillis() } }
        single<FreshnessGate> { DataStoreFreshnessGate(get(), get()) }
    }

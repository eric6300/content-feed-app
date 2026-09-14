package com.eric.contentfeed.core.freshness

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import kotlin.time.Duration

class DataStoreFreshnessGate(
    private val dataStore: DataStore<Preferences>,
    private val clock: EpochClock,
) : FreshnessGate {
    override suspend fun isStale(
        key: String,
        ttl: Duration,
    ): Boolean {
        require(key.isNotBlank()) { "Freshness key must not be blank" }
        require(!ttl.isNegative()) { "Freshness TTL must not be negative" }

        val lastFetched = dataStore.data.first()[preferenceKey(key)] ?: return true
        val age = clock.nowEpochMillis() - lastFetched
        return age < 0 || age >= ttl.inWholeMilliseconds
    }

    override suspend fun markFetched(key: String) {
        require(key.isNotBlank()) { "Freshness key must not be blank" }
        dataStore.edit { preferences ->
            preferences[preferenceKey(key)] = clock.nowEpochMillis()
        }
    }

    private fun preferenceKey(key: String): Preferences.Key<Long> = longPreferencesKey("freshness.$key")
}

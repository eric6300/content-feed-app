package com.eric.contentfeed.core.freshness

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.time.Duration.Companion.minutes

class DataStoreFreshnessGateTest {
    private lateinit var dataStoreFile: File
    private lateinit var clock: MutableTestClock
    private lateinit var gate: DataStoreFreshnessGate

    @Before
    fun setUp() {
        dataStoreFile = File.createTempFile("content-feed-freshness", ".preferences_pb").also(File::delete)
        clock = MutableTestClock(1_000)
        val dataStore = PreferenceDataStoreFactory.create { dataStoreFile }
        gate = DataStoreFreshnessGate(dataStore, clock)
    }

    @After
    fun tearDown() {
        dataStoreFile.delete()
    }

    @Test
    fun missingTimestampIsStale() =
        runTest {
            assertTrue(gate.isStale("articles", 10.minutes))
        }

    @Test
    fun timestampWithinTtlIsFreshAndExactTtlIsStale() =
        runTest {
            gate.markFetched("articles")
            clock.now = 1_000 + 10.minutes.inWholeMilliseconds - 1
            assertFalse(gate.isStale("articles", 10.minutes))

            clock.now++
            assertTrue(gate.isStale("articles", 10.minutes))
        }

    @Test
    fun futureTimestampIsTreatedAsStale() =
        runTest {
            gate.markFetched("articles")
            clock.now = 900

            assertTrue(gate.isStale("articles", 10.minutes))
        }

    @Test
    fun manualRefreshBypassesTtlByJustMarkingFetchedAgain() =
        runTest {
            gate.markFetched("articles")
            clock.now += 1.minutes.inWholeMilliseconds
            assertFalse(gate.isStale("articles", 10.minutes))

            // A manual refresh doesn't consult isStale at all — it always refetches, then
            // marks fetched again so the freshness window restarts from the new time.
            gate.markFetched("articles")
            clock.now += 9.minutes.inWholeMilliseconds + 999
            assertFalse(gate.isStale("articles", 10.minutes))
        }

    @Test
    fun corruptedPreferencesFileRecoversAsStaleInsteadOfCrashing() =
        runTest {
            // Not a valid preferences protobuf — simulates a file truncated by a kill
            // during write, or low storage.
            dataStoreFile.writeBytes(byteArrayOf(1, 2, 3, 4, 5))
            val recoveringDataStore =
                PreferenceDataStoreFactory.create(
                    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                ) { dataStoreFile }
            val recoveringGate = DataStoreFreshnessGate(recoveringDataStore, clock)

            assertTrue(recoveringGate.isStale("articles", 10.minutes))
        }

    @Test
    fun distinctKeysTrackFreshnessIndependently() =
        runTest {
            gate.markFetched("weather")
            assertTrue(gate.isStale("articles", 10.minutes))
            assertFalse(gate.isStale("weather", 10.minutes))
        }

    private class MutableTestClock(
        var now: Long,
    ) : EpochClock {
        override fun nowEpochMillis(): Long = now
    }
}

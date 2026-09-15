package com.eric.contentfeed.feed.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class DataStoreFeedCursorStoreTest {
    private lateinit var dataStoreFile: File
    private lateinit var cursorStore: DataStoreFeedCursorStore

    @Before
    fun setUp() {
        dataStoreFile = File.createTempFile("content-feed-cursor", ".preferences_pb").also(File::delete)
        cursorStore = DataStoreFeedCursorStore(PreferenceDataStoreFactory.create { dataStoreFile })
    }

    @After
    fun tearDown() {
        dataStoreFile.delete()
    }

    @Test
    fun unsetOffsetReadsAsZero() =
        runTest {
            assertEquals(0, cursorStore.readNextOffset())
        }

    @Test
    fun writtenOffsetIsReadBack() =
        runTest {
            cursorStore.writeNextOffset(40)

            assertEquals(40, cursorStore.readNextOffset())
        }
}

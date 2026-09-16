package com.eric.contentfeed.feed.presentation.mvi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MviViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun effectsCanBeCollectedAgainAfterTheRouteIsRecreated() =
        runTest(testDispatcher) {
            val viewModel = TestViewModel()

            val firstEffect = async { viewModel.effects.first() }
            runCurrent()
            viewModel.onEvent("first")
            advanceUntilIdle()
            assertEquals("first", firstEffect.await())

            val secondEffect = async { viewModel.effects.first() }
            runCurrent()
            viewModel.onEvent("second")
            advanceUntilIdle()
            assertEquals("second", secondEffect.await())
        }

    private class TestViewModel : MviViewModel<Unit, String, String>(Unit) {
        override suspend fun handleEvent(event: String) {
            emitEffect(event)
        }
    }
}

package com.eric.contentfeed.feed.presentation.viewmodel

import app.cash.turbine.test
import com.eric.contentfeed.core.connectivity.ConnectivityStatus
import com.eric.contentfeed.feed.domain.usecase.ObserveConnectivityUseCase
import com.eric.contentfeed.feed.presentation.contract.ConnectivityContract
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivityViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var connectivity: MutableStateFlow<ConnectivityStatus>
    private lateinit var viewModel: ConnectivityViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        connectivity = MutableStateFlow(ConnectivityStatus.Unknown)
        val observeConnectivity = mockk<ObserveConnectivityUseCase>()
        every { observeConnectivity() } returns connectivity
        viewModel = ConnectivityViewModel(observeConnectivity)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun offlineToOnlineEmitsBackOnlineOnceAndMirrorsLatestStatus() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            viewModel.effects.test {
                connectivity.value = ConnectivityStatus.Offline
                advanceUntilIdle()
                assertEquals(
                    ConnectivityContract.State(ConnectivityStatus.Offline),
                    viewModel.state.value,
                )

                connectivity.value = ConnectivityStatus.Online
                advanceUntilIdle()
                assertEquals(ConnectivityContract.Effect.BackOnline, awaitItem())
                assertEquals(
                    ConnectivityContract.State(ConnectivityStatus.Online),
                    viewModel.state.value,
                )

                connectivity.value = ConnectivityStatus.Online
                advanceUntilIdle()
                expectNoEvents()
            }
        }

    @Test
    fun unknownToOnlineDoesNotEmitBackOnline() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            viewModel.effects.test {
                connectivity.value = ConnectivityStatus.Online
                advanceUntilIdle()

                expectNoEvents()
                assertEquals(
                    ConnectivityContract.State(ConnectivityStatus.Online),
                    viewModel.state.value,
                )
            }
        }
}

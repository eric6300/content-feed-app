package com.eric.contentfeed.core.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

/** Android implementation backed by the default network callback. */
class AndroidConnectivityObserver(
    context: Context,
) : ConnectivityObserver {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val observerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val status =
        observeStatus()
            .stateIn(
                scope = observerScope,
                started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
                initialValue = currentStatus(),
            )

    private fun observeStatus(): Flow<ConnectivityStatus> =
        callbackFlow {
            val callback =
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        trySend(currentStatus())
                    }

                    override fun onLost(network: Network) {
                        trySend(currentStatus())
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities,
                    ) {
                        trySend(currentStatus())
                    }
                }

            connectivityManager.registerDefaultNetworkCallback(callback)
            awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
        }.distinctUntilChanged()

    private fun currentStatus(): ConnectivityStatus {
        val capabilities =
            connectivityManager.activeNetwork?.let(connectivityManager::getNetworkCapabilities)
        val isValidated =
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return if (isValidated) ConnectivityStatus.Online else ConnectivityStatus.Offline
    }
}

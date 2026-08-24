package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkMonitor(context: Context) {
    private val TAG = "NetworkMonitor"
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(checkInitialConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            updateConnectivity()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            updateConnectivity()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } else {
                true
            }
            _isOnline.value = hasInternet && isValidated
        }

        override fun onUnavailable() {
            super.onUnavailable()
            _isOnline.value = false
        }
    }

    init {
        registerCallback()
    }

    private fun registerCallback() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register network callback: ${e.message}")
            _isOnline.value = checkInitialConnectivity()
        }
    }

    private fun checkInitialConnectivity(): Boolean {
        val cm = connectivityManager ?: return true // Safe fallback
        return try {
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } else {
                true
            }
            hasInternet && isValidated
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check initial network state: ${e.message}")
            true
        }
    }

    fun isConnected(): Boolean {
        return checkInitialConnectivity()
    }

    private fun updateConnectivity() {
        _isOnline.value = checkInitialConnectivity()
    }
}

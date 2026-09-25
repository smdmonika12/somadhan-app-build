package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Real-time network connectivity observer using Android's ConnectivityManager and NetworkCallback.
 * Monitors internet availability throughout the app lifecycle.
 */
class NetworkConnectivityObserver(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    /**
     * Synchronously checks if there is an active network with internet capability right now.
     *
     * [বাগ ফিক্স] আগে এই ফাংশনের একদম শেষে থাকা fallback ছিল `return true` (কমেন্ট: "prevent
     * false-positive lockouts in emulator/preview environments") — অর্থাৎ `cm.activeNetwork`/
     * `activeNetworkInfo`/`allNetworks` কোনোটাতেই সংযোগ না পেলেও (যেমন airplane mode-এ, যখন
     * `activeNetwork` সত্যিই `null` আর `allNetworks` খালি) এই ফাংশন সবসময় `true` রিটার্ন করত।
     * এর ফলে `isOnline` কার্যত সবসময় `true`-ই থাকত, Strict Offline Block টগল ON থাকলেও
     * `NoInternetOverlay`-এর `isVisible = !isOnline && ...` কখনো `true` হতো না — টগলের
     * ব্রাঞ্চিং লজিক (`MainActivity.kt`) নিজে সঠিক ছিল, কিন্তু এই আন্ডারলাইং ফাংশনটাই ভুল সিগন্যাল
     * দিচ্ছিল। fallback এখন `false` (সত্যিকারের "সংযোগ নেই" অবস্থা প্রতিফলিত করে) — উপরের তিনটা
     * চেক (activeNetwork capabilities, deprecated activeNetworkInfo, allNetworks loop) কোনো একটাতে
     * বাস্তব সংযোগ পেলে তখনই `true` রিটার্ন হবে, নাহলে `false`।
     */
    fun isCurrentlyConnected(): Boolean {
        val cm = connectivityManager ?: return true
        val activeNetwork = cm.activeNetwork
        if (activeNetwork != null) {
            val capabilities = cm.getNetworkCapabilities(activeNetwork)
            if (capabilities != null) {
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    return true
                }
            }
        }
        @Suppress("DEPRECATION")
        val activeInfo = cm.activeNetworkInfo
        if (activeInfo != null && (activeInfo.isConnectedOrConnecting || activeInfo.isConnected)) {
            return true
        }
        for (net in cm.allNetworks) {
            val caps = cm.getNetworkCapabilities(net)
            if (caps != null && (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                                 caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
                                 caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                 caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                 caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                                 caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN))) {
                return true
            }
        }
        // কোনো active/known নেটওয়ার্কে ইন্টারনেট-সক্ষম capability পাওয়া যায়নি — প্রকৃতপক্ষে অফলাইন।
        return false
    }

    /**
     * Flow that emits real-time connectivity status (true = connected, false = disconnected).
     */
    val isConnectedFlow: Flow<Boolean> = callbackFlow {
        val cm = connectivityManager
        if (cm == null) {
            trySend(true)
            close()
            return@callbackFlow
        }

        // [বাগ ফিক্স] আগে এখানে কমেন্টে "Send initial state immediately based on real-time check"
        // লেখা থাকলেও আসলে hardcoded trySend(true) ছিল (real-time চেক করাই হতো না) — একই ভুল
        // onAvailable()/onCapabilitiesChanged()-এও ছিল (নেটওয়ার্ক callback fire হলেই hardcoded
        // true, capability/validation আসলে আছে কিনা যাচাই না করে)। এখন সবগুলো জায়গায়
        // isCurrentlyConnected()-এর real-time রেজাল্ট ব্যবহার করা হচ্ছে, ঠিক onLost()/
        // onUnavailable()-এ যেভাবে আগে থেকেই হতো।
        trySend(isCurrentlyConnected())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(isCurrentlyConnected())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(isCurrentlyConnected())
            }

            override fun onLost(network: Network) {
                trySend(isCurrentlyConnected())
            }

            override fun onUnavailable() {
                trySend(isCurrentlyConnected())
            }
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                cm.registerDefaultNetworkCallback(callback)
            } else {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                cm.registerNetworkCallback(request, callback)
            }
        } catch (e: Exception) {
            // Fallback emission
            trySend(true)
        }

        awaitClose {
            try {
                cm.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
    }.distinctUntilChanged()
}


package com.altmansoftwaredesign.hotspotwidget.repository

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.TetheringManager
import android.os.Build
import com.altmansoftwaredesign.hotspotwidget.model.HotspotState
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface IHotspotRepository {
    suspend fun getHotspotState(): HotspotState
    suspend fun enableHotspot(): Boolean
    suspend fun disableHotspot(): Boolean
}

class HotspotRepository(private val context: Context) : IHotspotRepository {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val tetheringManager =
        context.getSystemService(Context.TETHERING_SERVICE) as TetheringManager

    @SuppressLint("MissingPermission")
    override suspend fun getHotspotState(): HotspotState {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val isEnabled = queryHotspotStateApi31()
                HotspotState(isEnabled = isEnabled)
            } catch (e: Exception) {
                HotspotState(isEnabled = false, error = e.message)
            }
        } else {
            HotspotState(isEnabled = false)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun enableHotspot(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                suspendCancellableCoroutine { continuation ->
                    tetheringManager.startTethering(
                        TetheringManager.TETHERING_WIFI,
                        { continuation.resume(true) },
                        { continuation.resume(false) }
                    )
                }
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun disableHotspot(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                suspendCancellableCoroutine { continuation ->
                    tetheringManager.stopTethering(
                        TetheringManager.TETHERING_WIFI,
                        { continuation.resume(true) },
                        { continuation.resume(false) }
                    )
                }
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun queryHotspotStateApi31(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            tetheringManager.registerTetheringEventCallback(
                { continuation.resume(false) },
                object : TetheringManager.TetheringEventCallback {
                    override fun onTetherableStateChanged(
                        downstreamWifiIterable: Iterable<String>,
                        upstreamNetworks: Iterable<Int>,
                        tetheredIterable: Iterable<String>
                    ) {
                        val isEnabled = tetheredIterable.any { it.startsWith("wlan") }
                        continuation.resume(isEnabled)
                    }
                }
            )
        }
    }
}

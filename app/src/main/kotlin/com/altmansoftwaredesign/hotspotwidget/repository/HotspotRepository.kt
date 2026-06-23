package com.altmansoftwaredesign.hotspotwidget.repository

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import com.altmansoftwaredesign.hotspotwidget.model.HotspotState
import kotlinx.coroutines.delay

interface IHotspotRepository {
    suspend fun getHotspotState(): HotspotState
    suspend fun enableHotspot(): Boolean
    suspend fun disableHotspot(): Boolean
}

class HotspotRepository(private val context: Context) : IHotspotRepository {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @SuppressLint("MissingPermission")
    override suspend fun getHotspotState(): HotspotState {
        return try {
            val isTethered = isHotspotEnabled()
            HotspotState(isEnabled = isTethered)
        } catch (e: Exception) {
            HotspotState(isEnabled = false, error = e.message)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun enableHotspot(): Boolean {
        return try {
            val method = connectivityManager.javaClass.getDeclaredMethod(
                "startTethering",
                Int::class.java,
                Boolean::class.java,
                Class.forName("android.net.ConnectivityManager\$OnStartTetheringCallback")
            )
            method.isAccessible = true
            method.invoke(connectivityManager, 0, true, null)
            delay(1000)
            true
        } catch (e: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun disableHotspot(): Boolean {
        return try {
            val method = connectivityManager.javaClass.getDeclaredMethod("stopTethering", Int::class.java)
            method.isAccessible = true
            method.invoke(connectivityManager, 0)
            delay(1000)
            true
        } catch (e: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun isHotspotEnabled(): Boolean {
        return try {
            val method = connectivityManager.javaClass.getDeclaredMethod("getTetheredIfaces")
            method.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val tethered = method.invoke(connectivityManager) as Array<String>
            tethered.any { it.startsWith("wlan") || it.startsWith("ap") }
        } catch (e: Exception) {
            false
        }
    }
}

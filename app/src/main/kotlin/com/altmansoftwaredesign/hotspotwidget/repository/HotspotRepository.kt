package com.altmansoftwaredesign.hotspotwidget.repository

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import com.altmansoftwaredesign.hotspotwidget.model.HotspotState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

interface IHotspotRepository {
    suspend fun getHotspotState(): HotspotState
    suspend fun enableHotspot(): Boolean
    suspend fun disableHotspot(): Boolean
}

/**
 * Toggles and queries the Wi-Fi hotspot.
 *
 * Android does not expose a public API for programmatically toggling tethering,
 * so we reach the hidden ConnectivityManager methods via reflection. This is the
 * only option on API 31+ for a non-system app and is inherently fragile: the
 * private signatures may change in future Android releases. The reflection lint
 * and spell-check warnings below are suppressed deliberately for that reason.
 */
class HotspotRepository(context: Context) : IHotspotRepository {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @SuppressLint("MissingPermission")
    override suspend fun getHotspotState(): HotspotState {
        return try {
            HotspotState(isEnabled = isHotspotEnabled())
        } catch (e: Exception) {
            HotspotState(isEnabled = false, error = e.message)
        }
    }

    @SuppressLint("MissingPermission", "DiscouragedPrivateApi", "SoonBlockedPrivateApi")
    override suspend fun enableHotspot(): Boolean {
        return try {
            val method = connectivityManager.javaClass.getDeclaredMethod(
                "startTethering",
                Int::class.java,
                Boolean::class.java,
                Class.forName("android.net.ConnectivityManager${'$'}OnStartTetheringCallback")
            )
            method.isAccessible = true
            method.invoke(connectivityManager, 0, true, null)
            delay(1.seconds)
            true
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission", "DiscouragedPrivateApi", "SoonBlockedPrivateApi")
    override suspend fun disableHotspot(): Boolean {
        return try {
            val method =
                connectivityManager.javaClass.getDeclaredMethod("stopTethering", Int::class.java)
            method.isAccessible = true
            method.invoke(connectivityManager, 0)
            delay(1.seconds)
            true
        } catch (_: Exception) {
            false
        }
    }

    // "getTetheredIfaces" is the real (hidden) ConnectivityManager method name,
    // and "wlan"/"ap" are the actual OS network-interface name prefixes for the
    // Wi-Fi tethering interface (e.g. wlan0, ap0). They are external identifiers,
    // not typos, so spell-checking is suppressed for this function only.
    @Suppress("SpellCheckingInspection")
    @SuppressLint("MissingPermission", "DiscouragedPrivateApi", "SoonBlockedPrivateApi")
    private fun isHotspotEnabled(): Boolean {
        return try {
            val method = connectivityManager.javaClass.getDeclaredMethod("getTetheredIfaces")
            method.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val tethered = method.invoke(connectivityManager) as Array<String>
            tethered.any { it.startsWith("wlan") || it.startsWith("ap") }
        } catch (_: Exception) {
            false
        }
    }
}

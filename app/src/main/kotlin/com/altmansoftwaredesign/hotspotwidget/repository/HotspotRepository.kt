package com.altmansoftwaredesign.hotspotwidget.repository

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.altmansoftwaredesign.hotspotwidget.model.HotspotState
import kotlinx.coroutines.delay
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.concurrent.Executor
import kotlin.time.Duration.Companion.seconds

/**
 * Toggles and queries the Wi-Fi hotspot.
 *
 * Android exposes no public API for a non-system app to toggle tethering, so we
 * drive the hidden TetheringManager via reflection. start/stopTethering require
 * WRITE_SETTINGS (which the user grants once) or the system-only TETHER_PRIVILEGED.
 * This is inherently fragile — the hidden signatures may change in future Android
 * releases — hence the suppressed reflection/spell-check warnings.
 *
 * All non-public API this class touches, gathered in one place so a future
 * platform change can be audited without re-deriving where the risk is:
 *
 *  - `Context.getSystemService("tethering")` — the hidden TetheringManager
 *    service name. No `Context.TETHERING_SERVICE` constant is public.
 *  - `TetheringManager.TetheringRequest.Builder`, `.build()`, and the
 *    `StartTetheringCallback` interface — all `@hide`; reached via
 *    `Class.forName` and a dynamic [Proxy] because the callback is an
 *    interface, not an abstract class, and no public subclass exists.
 *  - `TetheringManager.startTethering(TetheringRequest, Executor,
 *    StartTetheringCallback)` and `TetheringManager.stopTethering(int)` —
 *    `@hide`, invoked via [startWifiTethering]/[stopWifiTethering].
 *  - `ConnectivityManager.getTetheredIfaces()` — `@hide`; the only way for a
 *    non-system app to read which interfaces are actively tethering.
 *  - `ConnectivityManager.getTetherableWifiRegexs()` — `@hide`; see
 *    [readWifiTetherPatterns] for why this is the preferred detection path
 *    over hardcoding interface-name prefixes.
 */
class HotspotRepository(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @SuppressLint("MissingPermission")
    suspend fun getHotspotState(): HotspotState {
        return try {
            HotspotState(isEnabled = isHotspotEnabled())
        } catch (e: Exception) {
            // Unknown, NOT off. Reporting "off" here would be a confident lie the
            // moment the hidden API changes under us.
            Log.w("HotspotRepo", "Could not read hotspot state", e)
            HotspotState(isEnabled = null)
        }
    }

    suspend fun enableHotspot(): Boolean {
        return try {
            startWifiTethering()
            delay(1.seconds)
            true
        } catch (e: Exception) {
            Log.w("HotspotRepo", "enableHotspot failed", e)
            false
        }
    }

    suspend fun disableHotspot(): Boolean {
        return try {
            stopWifiTethering()
            delay(1.seconds)
            true
        } catch (e: Exception) {
            Log.w("HotspotRepo", "disableHotspot failed", e)
            false
        }
    }

    // TetheringManager.startTethering(TetheringRequest, Executor, StartTetheringCallback).
    // The callback is an interface, so a Proxy satisfies it (an OnStartTetheringCallback
    // abstract class could not be). Type 0 == TETHERING_WIFI.
    @SuppressLint("PrivateApi", "DiscouragedPrivateApi", "SoonBlockedPrivateApi")
    private fun startWifiTethering() {
        val tetheringManager = appContext.getSystemService("tethering")
            ?: throw IllegalStateException("TetheringManager unavailable")

        val builderClass =
            Class.forName("android.net.TetheringManager" + '$' + "TetheringRequest" + '$' + "Builder")
        val builder = builderClass.getConstructor(Int::class.javaPrimitiveType)
            .newInstance(TETHERING_WIFI)
        val request = builderClass.getMethod("build").invoke(builder)
        val requestClass = Class.forName("android.net.TetheringManager" + '$' + "TetheringRequest")

        val callbackClass = Class.forName("android.net.TetheringManager" + '$' + "StartTetheringCallback")
        val callback = Proxy.newProxyInstance(
            callbackClass.classLoader,
            arrayOf(callbackClass),
            noopInvocationHandler
        )

        val startMethod = tetheringManager.javaClass.getMethod(
            "startTethering", requestClass, Executor::class.java, callbackClass
        )
        startMethod.invoke(tetheringManager, request, Executor { it.run() }, callback)
    }

    @SuppressLint("PrivateApi", "DiscouragedPrivateApi", "SoonBlockedPrivateApi")
    private fun stopWifiTethering() {
        val tetheringManager = appContext.getSystemService("tethering")
            ?: throw IllegalStateException("TetheringManager unavailable")
        val stopMethod =
            tetheringManager.javaClass.getMethod("stopTethering", Int::class.javaPrimitiveType)
        stopMethod.invoke(tetheringManager, TETHERING_WIFI)
    }

    // Proxy handler for the StartTetheringCallback interface. We don't act on the
    // result here — the widget/notification re-read the true state shortly after —
    // so callback methods (onTetheringStarted/onTetheringFailed) are no-ops.
    private val noopInvocationHandler = InvocationHandler { proxy, method, args ->
        when (method.name) {
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === args?.getOrNull(0)
            "toString" -> "StartTetheringCallbackProxy"
            else -> null
        }
    }

    // "getTetheredIfaces" is the real (hidden) ConnectivityManager method name.
    // Throws if the reflection fails; the caller turns that into an "unknown"
    // state. Do not swallow it into `false` — see [getHotspotState].
    @SuppressLint("MissingPermission", "PrivateApi", "DiscouragedPrivateApi", "SoonBlockedPrivateApi")
    private fun isHotspotEnabled(): Boolean {
        val method = connectivityManager.javaClass.getDeclaredMethod("getTetheredIfaces")
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val tethered = method.invoke(connectivityManager) as Array<String>

        val patterns = wifiTetherPatterns
        return if (patterns.isNotEmpty()) {
            tethered.any { iface -> patterns.any { it.matches(iface) } }
        } else {
            // The device didn't give us any regexes (see [readWifiTetherPatterns])
            // — fall back to the two Wi-Fi tethering naming schemes we've
            // actually observed: stock AOSP's "wlan0"/"ap0", and Samsung's
            // "swlan0". Matching "wlan" as a substring, not just a prefix,
            // catches both. Not a typo, hence the spell-check suppression.
            @Suppress("SpellCheckingInspection")
            tethered.any { it.startsWith("ap") || it.contains("wlan") }
        }
    }

    // Regexes describing which interface names count as "Wi-Fi tethering" on
    // THIS device, read from the OEM's own tethering config instead of us
    // guessing at naming schemes. This is the same mechanism
    // ConnectivityManager/Tethering use internally to classify a tethered
    // interface as Wi-Fi vs. USB vs. Bluetooth, so it already accounts for
    // OEM-specific names like Samsung's "swlan0" — no hardcoded prefix list
    // needed. Cached: the underlying resource (config_tether_wifi_regexs) is
    // static for the life of the process.
    //
    // "getTetherableWifiRegexs" is the real (hidden) ConnectivityManager
    // method name — not a typo. A failure here (method missing, or a
    // malformed pattern on some OEM build) falls back to the hardcoded
    // heuristic in [isHotspotEnabled] rather than reporting "unknown"; losing
    // the smarter detection is better than losing hotspot status entirely.
    private val wifiTetherPatterns: List<Regex> by lazy { readWifiTetherPatterns() }

    @Suppress("SpellCheckingInspection")
    @SuppressLint("PrivateApi", "DiscouragedPrivateApi", "SoonBlockedPrivateApi")
    private fun readWifiTetherPatterns(): List<Regex> {
        return try {
            val method = connectivityManager.javaClass.getDeclaredMethod("getTetherableWifiRegexs")
            method.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val regexes = method.invoke(connectivityManager) as Array<String>
            regexes.mapNotNull { pattern ->
                try {
                    Regex(pattern)
                } catch (e: Exception) {
                    Log.w("HotspotRepo", "Unparseable Wi-Fi tether regex: $pattern", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.w("HotspotRepo", "Could not read tetherable Wi-Fi regexes", e)
            emptyList()
        }
    }

    private companion object {
        const val TETHERING_WIFI = 0
    }
}

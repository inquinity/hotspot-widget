package com.altmansoftwaredesign.hotspotwidget.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.altmansoftwaredesign.hotspotwidget.model.BatteryState

interface IBatteryRepository {
    fun getBatteryState(): BatteryState
    fun getBatteryPercentage(): Int
}

class BatteryRepository(private val context: Context) : IBatteryRepository {
    override fun getBatteryState(): BatteryState {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, ifilter)

        return if (batteryStatus != null) {
            val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
            val temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            val chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL ||
                    (chargePlug == BatteryManager.BATTERY_PLUGGED_AC ||
                            chargePlug == BatteryManager.BATTERY_PLUGGED_USB ||
                            chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS)

            val percentage = (level.coerceAtLeast(0) * 100) / scale.coerceAtLeast(1)

            BatteryState(
                percentage = percentage,
                status = status,
                health = health,
                temperature = temperature,
                isCharging = isCharging
            )
        } else {
            BatteryState(
                percentage = 0,
                status = BatteryManager.BATTERY_STATUS_UNKNOWN,
                health = BatteryManager.BATTERY_HEALTH_UNKNOWN,
                temperature = 0,
                isCharging = false
            )
        }
    }

    override fun getBatteryPercentage(): Int = getBatteryState().percentage
}

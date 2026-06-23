package com.altmansoftwaredesign.hotspotwidget.receiver

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.altmansoftwaredesign.hotspotwidget.service.BatteryMonitorService
import com.altmansoftwaredesign.hotspotwidget.widget.HotspotWidgetProvider

/**
 * Restarts the battery monitor after a reboot, but only if a widget is actually
 * placed. BOOT_COMPLETED is one of the few broadcasts still allowed to start a
 * foreground service from the background.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(
            ComponentName(context, HotspotWidgetProvider::class.java)
        )
        if (ids.isNotEmpty()) {
            BatteryMonitorService.start(context)
        }
    }
}

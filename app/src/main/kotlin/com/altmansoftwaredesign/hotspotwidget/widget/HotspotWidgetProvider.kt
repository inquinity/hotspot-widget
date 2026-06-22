package com.altmansoftwaredesign.hotspotwidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.altmansoftwaredesign.hotspotwidget.R
import com.altmansoftwaredesign.hotspotwidget.repository.BatteryRepository
import com.altmansoftwaredesign.hotspotwidget.repository.HotspotRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class HotspotWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_TOGGLE_HOTSPOT -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    handleToggleClick(context, appWidgetId)
                }
            }
        }
    }

    private fun handleToggleClick(context: Context, appWidgetId: Int) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))

        val workRequest = OneTimeWorkRequestBuilder<WidgetToggleWorker>()
            .setInputData(workDataOf(KEY_APPWIDGET_ID to appWidgetId))
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    companion object {
        const val ACTION_TOGGLE_HOTSPOT = "com.altmansoftwaredesign.hotspotwidget.TOGGLE_HOTSPOT"
        const val KEY_APPWIDGET_ID = "appwidget_id"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            GlobalScope.launch {
                val hotspotRepo = HotspotRepository(context)
                val batteryRepo = BatteryRepository(context)

                val hotspotState = hotspotRepo.getHotspotState()
                val batteryState = batteryRepo.getBatteryState()

                val views = RemoteViews(context.packageName, R.layout.widget_layout)

                // Update hotspot status
                val hotspotIconRes = if (hotspotState.isEnabled) {
                    R.drawable.ic_hotspot_on
                } else {
                    R.drawable.ic_hotspot_off
                }
                val hotspotStatusText = if (hotspotState.isEnabled) {
                    R.string.hotspot_on
                } else {
                    R.string.hotspot_off
                }
                views.setImageViewResource(R.id.hotspot_icon, hotspotIconRes)
                views.setTextViewText(R.id.hotspot_status, context.getString(hotspotStatusText))

                // Update battery status
                val batteryIconRes = when {
                    batteryState.percentage > 60 -> R.drawable.ic_battery_high
                    batteryState.percentage > 20 -> R.drawable.ic_battery_medium
                    else -> R.drawable.ic_battery_low
                }
                views.setImageViewResource(R.id.battery_icon, batteryIconRes)
                views.setTextViewText(
                    R.id.battery_percentage,
                    "${batteryState.percentage}%"
                )

                // Set click listener
                val toggleIntent = Intent(context, HotspotWidgetProvider::class.java).apply {
                    action = ACTION_TOGGLE_HOTSPOT
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId,
                    toggleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.hotspot_icon, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}

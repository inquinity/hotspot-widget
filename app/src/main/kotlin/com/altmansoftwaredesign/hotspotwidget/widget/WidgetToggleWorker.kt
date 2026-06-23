package com.altmansoftwaredesign.hotspotwidget.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.altmansoftwaredesign.hotspotwidget.repository.HotspotRepository
import com.altmansoftwaredesign.hotspotwidget.widget.HotspotWidgetProvider.Companion.KEY_APPWIDGET_ID

class WidgetToggleWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val appWidgetId = inputData.getInt(KEY_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                return Result.failure()
            }

            val hotspotRepo = HotspotRepository(applicationContext)
            val currentState = hotspotRepo.getHotspotState()

            if (currentState.isEnabled) {
                hotspotRepo.disableHotspot()
            } else {
                hotspotRepo.enableHotspot()
            }

            // Always refresh the widget to reflect the true post-toggle state,
            // whether or not the toggle itself succeeded. This is a one-shot,
            // user-initiated action, so never auto-retry in the background.
            val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
            HotspotWidgetProvider.updateAppWidget(
                applicationContext,
                appWidgetManager,
                appWidgetId
            )

            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }
}

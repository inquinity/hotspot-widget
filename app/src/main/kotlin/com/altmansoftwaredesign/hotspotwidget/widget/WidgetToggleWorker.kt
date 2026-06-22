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

            val success = if (currentState.isEnabled) {
                hotspotRepo.disableHotspot()
            } else {
                hotspotRepo.enableHotspot()
            }

            if (success) {
                val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
                HotspotWidgetProvider.updateAppWidget(
                    applicationContext,
                    appWidgetManager,
                    appWidgetId
                )
            }

            if (success) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

package com.altmansoftwaredesign.hotspotwidget.ui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.appcompat.app.AlertDialog
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.altmansoftwaredesign.hotspotwidget.R
import com.altmansoftwaredesign.hotspotwidget.widget.HotspotWidgetProvider
import com.altmansoftwaredesign.hotspotwidget.widget.WidgetToggleWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.altmansoftwaredesign.hotspotwidget.repository.HotspotRepository

class ConfirmToggleActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        // Determine current state to phrase the dialog correctly
        CoroutineScope(Dispatchers.Main).launch {
            val isEnabled = withContext(Dispatchers.IO) {
                HotspotRepository(applicationContext).getHotspotState().isEnabled
            }
            showDialog(appWidgetId, isEnabled)
        }
    }

    private fun showDialog(appWidgetId: Int, isCurrentlyEnabled: Boolean) {
        val messageRes = if (isCurrentlyEnabled) {
            R.string.confirm_turn_off
        } else {
            R.string.confirm_turn_on
        }
        val confirmLabel = if (isCurrentlyEnabled) {
            getString(R.string.turn_off)
        } else {
            getString(R.string.turn_on)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.toggle_hotspot)
            .setMessage(messageRes)
            .setPositiveButton(confirmLabel) { _, _ ->
                vibrate()
                enqueueToggle(appWidgetId)
                finish()
            }
            .setNegativeButton(R.string.cancel) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun enqueueToggle(appWidgetId: Int) {
        val workRequest = OneTimeWorkRequestBuilder<WidgetToggleWorker>()
            .setInputData(workDataOf(HotspotWidgetProvider.KEY_APPWIDGET_ID to appWidgetId))
            .build()
        WorkManager.getInstance(applicationContext).enqueue(workRequest)
    }

    private fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        }
    }
}

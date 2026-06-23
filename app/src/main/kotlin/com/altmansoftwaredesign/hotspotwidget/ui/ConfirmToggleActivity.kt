package com.altmansoftwaredesign.hotspotwidget.ui

import android.app.Activity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.appcompat.app.AlertDialog
import com.altmansoftwaredesign.hotspotwidget.R
import com.altmansoftwaredesign.hotspotwidget.repository.HotspotRepository
import com.altmansoftwaredesign.hotspotwidget.service.BatteryMonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConfirmToggleActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // A widget tap is an allowed context to start a foreground service, so
        // ensure the live battery/hotspot monitor is running from here.
        BatteryMonitorService.start(this)

        // Determine current state to phrase the dialog correctly.
        CoroutineScope(Dispatchers.Main).launch {
            val isEnabled = withContext(Dispatchers.IO) {
                HotspotRepository(applicationContext).getHotspotState().isEnabled
            }
            showDialog(isEnabled)
        }
    }

    private fun showDialog(isCurrentlyEnabled: Boolean) {
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
                BatteryMonitorService.toggle(this)
                finish()
            }
            .setNegativeButton(R.string.cancel) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        }
    }
}

package com.altmansoftwaredesign.hotspotwidget.ui

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.altmansoftwaredesign.hotspotwidget.R

/**
 * Tiny transparent dialog shown when the battery side of the widget is tapped.
 * Displays the app name and version.
 */
class AboutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val version = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (_: Exception) {
            "?"
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage(getString(R.string.about_version, version))
            .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }
}

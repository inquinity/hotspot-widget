package com.altmansoftwaredesign.hotspotwidget

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Small first-run / setup screen. The app is driven entirely from the home-screen
 * widget; this screen just explains that and lets the user grant the one-time
 * WRITE_SETTINGS permission the hotspot toggle needs.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var statusView: TextView
    private lateinit var grantButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusView = findViewById(R.id.permission_status)
        grantButton = findViewById(R.id.grant_button)
        grantButton.setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check on return from the settings screen.
        val granted = Settings.System.canWrite(this)
        statusView.text = getString(
            if (granted) R.string.setup_status_granted else R.string.setup_status_needed
        )
        grantButton.visibility = if (granted) android.view.View.GONE else android.view.View.VISIBLE
    }
}

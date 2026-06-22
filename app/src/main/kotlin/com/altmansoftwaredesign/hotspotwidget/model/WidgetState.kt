package com.altmansoftwaredesign.hotspotwidget.model

data class HotspotState(
    val isEnabled: Boolean,
    val lastUpdate: Long = System.currentTimeMillis(),
    val error: String? = null
)

data class BatteryState(
    val percentage: Int,
    val status: Int,
    val health: Int,
    val temperature: Int,
    val isCharging: Boolean
)

data class WidgetDisplayState(
    val hotspot: HotspotState,
    val battery: BatteryState
)

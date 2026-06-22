# Hotspot Widget

A minimal Android 12+ widget that displays hotspot status and battery percentage, with the ability to toggle hotspot via confirmation dialog.

## Features

- **2x2 Compact Widget** — minimal footprint on home screen
- **Hotspot Status Display** — shows current hotspot state (on/off) with icon
- **Battery Percentage** — displays current battery level with color indicator
- **One-click Toggle** — taps widget icon to initiate hotspot toggle
- **Confirmation Dialog** — explicit buttons to confirm/cancel toggle action
- **Auto-dismiss** — dialog closes automatically on successful toggle
- **Material You Theming** — automatically adapts to device theme

## Requirements

- **Android 12 or later** (uses TetheringManager API 31+)
- **Gradle 8.0+**
- **Android Studio** (or CLI with gradle wrapper)
- **OnePlus Nord N200** (or compatible Android 12+ device)

## Build & Installation

### 1. Build the APK
```bash
./gradlew assembleDebug
```

### 2. Install via ADB
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. Add Widget to Home Screen
- Long-press home screen → "Add widget"
- Search for "Hotspot Widget"
- Place on home screen

## Permissions

The app requires these runtime permissions:
- `CHANGE_NETWORK_STATE` — to toggle hotspot on/off
- `ACCESS_NETWORK_STATE` — to query current hotspot state
- `VIBRATE` — for haptic feedback on widget click (optional)

Grant permissions in Settings → Apps → Hotspot Widget → Permissions.

## Architecture

### Core Components
- **HotspotWidgetProvider** — main widget lifecycle manager
- **WidgetToggleWorker** — handles hotspot state changes
- **HotspotRepository** — queries/modifies hotspot state (Android 12+ TetheringManager)
- **BatteryRepository** — queries battery level and status
- **WidgetState** — data models for state management

### Layout
- **2x2 widget** (110dp x 110dp) stacked vertically
- Top half: Hotspot status + icon
- Divider
- Bottom half: Battery percentage + icon

## Future Features

See [future_ideas.md](future_ideas.md) for planned enhancements including:
- Display hotspot SSID
- Show connected device count
- Haptic feedback on toggle
- Battery cutoff threshold
- Data usage monitoring

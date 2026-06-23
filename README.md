# Hotspot Widget

A minimal Android 12+ home-screen widget that shows Wi-Fi hotspot status and
battery percentage, and toggles the hotspot on/off (with a confirmation dialog)
when tapped.

## Features

- **Compact horizontal widget** — hotspot status on the left, battery on the
  right, separated by a divider; resizable
- **Hotspot status** — on/off, with icon
- **Battery percentage** — with a colour indicator (green/orange/red by level)
- **Tap to toggle** — tapping the widget opens a confirmation dialog before
  changing the hotspot; the dialog wording adapts to the current state
  (“Turn on?” / “Turn off?”)
- **Live updates** — a small foreground service keeps the battery and hotspot
  readings current, including when the hotspot is changed from Quick Settings or
  elsewhere. Its required ongoing notification is made useful, showing
  `Hotspot: On/Off · Battery NN%`.

## Requirements

- **Android 12 or later** (uses the hidden `TetheringManager` API, API 31+)
- **JDK 17**, **Android Studio** (or the bundled Gradle wrapper)
- Built/tested against a **OnePlus Nord N200** (DE2117, Android 12)

## Build & install

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then add the widget: long-press the home screen → **Widgets** → **Hotspot
Widget** → drag it onto a home screen.

## First run — one-time permission

Toggling tethering requires the **WRITE_SETTINGS** (“Modify system settings”)
permission. The first time you tap **Turn On/Off**, the app sends you to the
grant screen; allow it once, then tap the widget again. (Opening the app icon
shows a small setup screen with the current permission status.)

## Permissions

- `WRITE_SETTINGS` — required to turn tethering on/off (user-granted once)
- `CHANGE_NETWORK_STATE` / `ACCESS_NETWORK_STATE` — query/control tethering
- `FOREGROUND_SERVICE` (+ `FOREGROUND_SERVICE_SPECIAL_USE`) — run the live monitor
- `RECEIVE_BOOT_COMPLETED` — restart the monitor after a reboot (only if a
  widget is placed)
- `POST_NOTIFICATIONS` — show the monitor notification (only enforced on
  Android 13+)
- `VIBRATE` — haptic tick on confirm

## Architecture

- **HotspotWidgetProvider** — draws the widget; starts the monitor only from
  allowed contexts (Android 12+ forbids starting a foreground service from a
  broadcast receiver), and stops it in `onDisabled`
- **ConfirmToggleActivity** — transparent dialog activity; checks WRITE_SETTINGS,
  then drives the toggle through the service (a widget tap is an allowed
  foreground-service start context)
- **BatteryMonitorService** — foreground service; listens for
  `ACTION_BATTERY_CHANGED` and `TETHER_STATE_CHANGED`, refreshes the widget and
  notification, and performs the actual toggle
- **HotspotRepository** — reflects into `TetheringManager.startTethering` /
  `stopTethering` (callback supplied via a `Proxy`) and reads live state via
  `getTetheredIfaces()`
- **BatteryRepository** — reads battery level/status
- **BootReceiver** — restarts the monitor on boot if a widget exists

### Layout

Horizontal 3×1 widget: hotspot icon + status on the left, vertical divider,
battery icon + percentage on the right.

## Future ideas

See [future_ideas.md](future_ideas.md) — SSID, connected-device count, haptics,
battery cutoff, data-usage monitoring.

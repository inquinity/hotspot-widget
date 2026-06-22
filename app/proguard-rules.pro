# Keep AppWidget provider
-keep class * extends android.appwidget.AppWidgetProvider {
  <init>();
}

# Keep WorkManager
-keep class * extends androidx.work.Worker {
  <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Keep BroadcastReceiver
-keep class * extends android.content.BroadcastReceiver {
  <init>();
}

package com.example.notifsimplifier.service

import android.app.Notification
import android.app.PendingIntent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.notifsimplifier.data.AppDatabase
import com.example.notifsimplifier.data.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (packageName == "com.example.notifsimplifier") return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        // OTPs must always show normally — check before any DB lookup.
        if (OtpDetector.isOtp(title, text)) return

        val contentIntent = sbn.notification.contentIntent

        scope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val settings = db.appSettingDao().getByPackage(packageName)

            // App not yet in settings table → user hasn't reviewed it; let it through.
            if (settings == null) return@launch

            // Nothing is redirected until the user explicitly opts the app in.
            if (!settings.isRedirected) return@launch

            val rowId = db.notificationDao().insert(
                NotificationEntity(
                    appName = packageName,
                    title = title,
                    text = text,
                    timestamp = System.currentTimeMillis()
                )
            )
            if (contentIntent != null) pendingIntents[rowId] = contentIntent
            cancelNotification(sbn.key)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) = Unit

    companion object {
        // In-memory store of content intents keyed by DB row ID.
        // Survives for the process lifetime; cleared when the user clears all notifications.
        val pendingIntents = mutableMapOf<Long, PendingIntent>()
    }
}

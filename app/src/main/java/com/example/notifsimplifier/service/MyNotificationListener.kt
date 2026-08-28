package com.example.notifsimplifier.service

import android.app.Notification
import android.service.notification.StatusBarNotification
import android.service.notification.NotificationListenerService
import com.example.notifsimplifier.data.AppDatabase
import com.example.notifsimplifier.data.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Add package names here to skip capturing them (e.g. your own app,
    // or ongoing/ silent notifications you never want logged).
    private val ignoredPackages = setOf(
        "com.example.notifsimplifier"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (packageName in ignoredPackages) return

        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

        // Skip empty/ongoing notifications (foreground service notifications, etc.)
        if (title.isBlank() && text.isBlank()) return

        val entity = NotificationEntity(
            appName = packageName,
            title = title,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        scope.launch {
            AppDatabase.getInstance(applicationContext).notificationDao().insert(entity)
        }

        // Cancel the original so it never shows a banner/badge/sound on the
        // system UI — this is the main lever for reducing dopamine hits.
        cancelNotification(sbn.key)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // No-op: we already stored our own copy when it was posted.
    }
}

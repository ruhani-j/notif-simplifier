package com.example.notifsimplifier.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.notifsimplifier.data.AppDatabase
import com.example.notifsimplifier.data.AppSettingEntity
import com.example.notifsimplifier.data.NotificationEntity
import com.example.notifsimplifier.data.NotifMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

        val otpBypassEnabled = applicationContext
            .getSharedPreferences("prefs", Context.MODE_PRIVATE)
            .getBoolean("otp_bypass", true)
        if (otpBypassEnabled && OtpDetector.isOtp(title, text)) return

        val contentIntent = sbn.notification.contentIntent

        scope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val settings = db.appSettingDao().getByPackage(packageName)

            if (settings == null) {
                val displayName = runCatching {
                    applicationContext.packageManager
                        .getApplicationLabel(
                            applicationContext.packageManager.getApplicationInfo(packageName, 0)
                        ).toString()
                }.getOrDefault(packageName)

                db.appSettingDao().insertIfAbsent(
                    AppSettingEntity(
                        packageName = packageName,
                        displayName = displayName,
                        mode = NotifMode.UNSET.name
                    )
                )
                _pendingPrompts.update { if (packageName !in it) it + packageName else it }
                // Notification passes through normally until the user sets a mode.
                return@launch
            }

            when (NotifMode.valueOf(settings.mode)) {
                NotifMode.UNSET -> {
                    // Already in DB but not yet configured — re-queue the prompt if needed.
                    _pendingPrompts.update { if (packageName !in it) it + packageName else it }
                }
                NotifMode.REDIRECT -> {
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
                NotifMode.INSTANT -> Unit // show normally
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) = Unit

    companion object {
        val pendingIntents = mutableMapOf<Long, PendingIntent>()

        private val _pendingPrompts = MutableStateFlow<List<String>>(emptyList())
        val pendingPrompts: StateFlow<List<String>> = _pendingPrompts.asStateFlow()

        fun addPendingPrompts(packages: List<String>) {
            _pendingPrompts.update { existing -> (existing + packages).distinct() }
        }

        fun clearPrompt(packageName: String) {
            _pendingPrompts.update { it - packageName }
        }
    }
}

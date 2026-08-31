package com.example.notifsimplifier.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Parcel
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

        val prefs = applicationContext.getSharedPreferences("prefs", Context.MODE_PRIVATE)

        val neverRedirect = prefs.getStringSet("never_redirect_packages", emptySet()) ?: emptySet()
        if (packageName in neverRedirect) return

        if (prefs.getBoolean("otp_bypass", true) && OtpDetector.isOtp(title, text)) return

        if (prefs.getBoolean("ongoing_filter", true) &&
            SmartFilters.isOngoing(sbn)) return

        val contentIntent = sbn.notification.contentIntent

        scope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val settings = db.appSettingDao().getByPackage(packageName)

            val effectiveMode: NotifMode
            if (settings == null) {
                val displayName = runCatching {
                    applicationContext.packageManager
                        .getApplicationLabel(
                            applicationContext.packageManager.getApplicationInfo(packageName, 0)
                        ).toString()
                }.getOrDefault(packageName)

                val defaultModeStr = prefs.getString("new_app_default", NotifMode.REDIRECT.name)
                    ?: NotifMode.REDIRECT.name
                val defaultMode = runCatching { NotifMode.valueOf(defaultModeStr) }
                    .getOrDefault(NotifMode.REDIRECT)

                db.appSettingDao().insertIfAbsent(
                    AppSettingEntity(
                        packageName = packageName,
                        displayName = displayName,
                        mode = defaultMode.name
                    )
                )
                effectiveMode = defaultMode
            } else {
                effectiveMode = runCatching { NotifMode.valueOf(settings.mode) }
                    .getOrDefault(NotifMode.UNSET)
            }

            when (effectiveMode) {
                NotifMode.UNSET -> {
                    // Not yet configured — apply system filter here so explicitly-configured apps bypass it.
                    if (prefs.getBoolean("system_filter", true) &&
                        SmartFilters.isSystemApp(applicationContext, packageName)) return@launch
                    _pendingPrompts.update { if (packageName !in it) it + packageName else it }
                }
                NotifMode.REDIRECT -> {
                    val importantBypassEnabled = prefs.getBoolean("important_bypass", true)
                    if (importantBypassEnabled && ImportantDetector.isImportant(title, text)) {
                        return@launch // show normally
                    }
                    val rowId = db.notificationDao().insert(
                        NotificationEntity(
                            appName = packageName,
                            title = title,
                            text = text,
                            timestamp = System.currentTimeMillis(),
                            intentBytes = contentIntent?.let { marshallIntent(it) }
                        )
                    )
                    if (contentIntent != null) pendingIntents[rowId] = contentIntent
                    cancelNotification(sbn.key)
                }
                NotifMode.INSTANT -> {
                    val marketingFilterEnabled = prefs.getBoolean("marketing_filter", false)
                    if (marketingFilterEnabled && MarketingDetector.isMarketing(title, text)) {
                        val rowId = db.notificationDao().insert(
                            NotificationEntity(
                                appName = packageName,
                                title = title,
                                text = text,
                                timestamp = System.currentTimeMillis(),
                                intentBytes = contentIntent?.let { marshallIntent(it) }
                            )
                        )
                        if (contentIntent != null) pendingIntents[rowId] = contentIntent
                        cancelNotification(sbn.key)
                    }
                    // Otherwise show normally.
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) = Unit

    private fun marshallIntent(pi: PendingIntent): ByteArray? {
        return try {
            val parcel = Parcel.obtain()
            pi.writeToParcel(parcel, 0)
            val bytes = parcel.marshall()
            parcel.recycle()
            bytes
        } catch (_: Exception) { null }
    }

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

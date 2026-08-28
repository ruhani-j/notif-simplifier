package com.example.notifsimplifier.service

import android.app.Notification
import android.content.Context
import android.content.pm.ApplicationInfo
import android.service.notification.StatusBarNotification

object SmartFilters {

    fun isSystemApp(context: Context, packageName: String): Boolean =
        runCatching {
            val flags = context.packageManager.getApplicationInfo(packageName, 0).flags
            (flags and ApplicationInfo.FLAG_SYSTEM) != 0
        }.getOrDefault(false)

    // Catches persistent/status notifications: music playback, navigation, downloads,
    // VPN, hotspot, calls, screen recording — anything meant to show continuously.
    fun isOngoing(sbn: StatusBarNotification): Boolean {
        val flags = sbn.notification.flags
        val extras = sbn.notification.extras
        val isOngoingFlag = (flags and Notification.FLAG_ONGOING_EVENT) != 0
        val hasProgressBar = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) > 0
        val isIndeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)
        // MediaStyle notifications (Spotify, music apps) may not set FLAG_ONGOING_EVENT
        val isMediaStyle = extras.getString(Notification.EXTRA_TEMPLATE) == "android.app.Notification\$MediaStyle"
        return isOngoingFlag || hasProgressBar || isIndeterminate || isMediaStyle
    }
}

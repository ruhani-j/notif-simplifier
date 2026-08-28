package com.example.notifsimplifier.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.notifsimplifier.MainActivity

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )

        val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Time to check in")
            .setContentText("You have notifications waiting in Notif Simplifier.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIF_ID, notification)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "reminder_channel"
        const val NOTIF_ID = 9001
        const val WORK_NAME = "app_reminder"
    }
}

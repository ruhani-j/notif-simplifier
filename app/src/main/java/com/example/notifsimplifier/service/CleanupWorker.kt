package com.example.notifsimplifier.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.notifsimplifier.data.AppDatabase

class CleanupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        AppDatabase.getInstance(applicationContext)
            .notificationDao()
            .deleteExpired(System.currentTimeMillis())
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "notif_cleanup"
    }
}

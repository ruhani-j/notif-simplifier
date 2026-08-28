package com.example.notifsimplifier.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appName: String,      // package name of the app that posted it
    val title: String,
    val text: String,
    val timestamp: Long       // System.currentTimeMillis() when captured
)

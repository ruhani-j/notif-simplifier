package com.example.notifsimplifier.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val packageName: String,
    val displayName: String,
    val mode: String = NotifMode.UNSET.name,
    // Legacy columns kept for DB compatibility — no longer used in app logic.
    val isRedirected: Boolean = false,
    val isAlwaysShowNormally: Boolean = false,
    val collectHistory: Boolean = false,
    // -2 = use global TTL setting, -1 = never expire, positive = hours
    val historyTtlHours: Int = -2
)
